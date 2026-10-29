package com.kestrel.rulesengine.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.core.time.TimeProvider;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import com.kestrel.rulesengine.api.dto.LocationAlertStatusResponse;
import com.kestrel.rulesengine.api.dto.LocationForecastResponse;
import com.kestrel.rulesengine.api.dto.LocationForecastResponse.Thresholds;
import com.kestrel.rulesengine.domain.ExceedanceCalculator;
import com.kestrel.rulesengine.domain.ExceedanceEvaluation;
import com.kestrel.rulesengine.domain.SeverityStateMachine;
import com.kestrel.rulesengine.domain.StateTransition;
import com.kestrel.rulesengine.infra.entity.AlertHistoryEntity;
import com.kestrel.rulesengine.infra.entity.AlertStateEntity;
import com.kestrel.rulesengine.infra.entity.ForecastSnapshotReplicaEntity;
import com.kestrel.rulesengine.infra.entity.MonitoredLocationReplicaEntity;
import com.kestrel.rulesengine.infra.publisher.AlertEventPublisher;
import com.kestrel.rulesengine.infra.repository.AlertHistoryRepository;
import com.kestrel.rulesengine.infra.repository.AlertStateRepository;
import com.kestrel.rulesengine.infra.repository.ForecastSnapshotReplicaRepository;
import com.kestrel.rulesengine.infra.repository.MonitoredLocationReplicaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RulesEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(RulesEvaluationService.class);

    private final AlertStateRepository alertStateRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MonitoredLocationReplicaRepository locationRepository;
    private final ForecastSnapshotReplicaRepository forecastRepository;
    private final AlertEventPublisher eventPublisher;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    private final ExceedanceCalculator exceedanceCalculator = new ExceedanceCalculator();
    private final SeverityStateMachine stateMachine = new SeverityStateMachine();

    public RulesEvaluationService(
            AlertStateRepository alertStateRepository,
            AlertHistoryRepository alertHistoryRepository,
            MonitoredLocationReplicaRepository locationRepository,
            ForecastSnapshotReplicaRepository forecastRepository,
            AlertEventPublisher eventPublisher,
            TimeProvider timeProvider,
            ObjectMapper objectMapper) {
        this.alertStateRepository = alertStateRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.locationRepository = locationRepository;
        this.forecastRepository = forecastRepository;
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void seedInitialPilotLocations() {
        if (locationRepository.count() == 0) {
            log.info("Seeding all 45 real Indian river stations from IndianRiverCatalog into MonitoredLocationReplica table");
            List<MonitoredLocationReplicaEntity> seeds = IndianRiverCatalog.ALL_45_STATIONS.stream()
                    .map(station -> new MonitoredLocationReplicaEntity(
                            station.locationId(),
                            station.name(),
                            station.state(),
                            station.latitude(),
                            station.longitude(),
                            station.riverName(),
                            station.threshold2yr(),
                            station.threshold5yr(),
                            station.threshold20yr(),
                            station.primaryLanguage(),
                            station.active()
                    ))
                    .toList();
            locationRepository.saveAll(seeds);
        }

        Instant now = timeProvider.now();
        LocalDate today = LocalDate.now();

        for (MonitoredLocationReplicaEntity loc : locationRepository.findAll()) {
            if (!alertStateRepository.existsById(loc.getLocationId())) {
                AlertStateEntity initial = new AlertStateEntity(
                        loc.getLocationId(),
                        loc.getName(),
                        Hazard.FLOOD,
                        Severity.NONE,
                        Severity.NONE,
                        0,
                        null,
                        Provenance.MODEL_ONLY,
                        null,
                        0.0,
                        now,
                        now,
                        null,
                        false
                );
                alertStateRepository.save(initial);
            }

            if (!forecastRepository.existsById(loc.getLocationId())) {
                List<ForecastDayStats> calmDays = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    calmDays.add(new ForecastDayStats(
                            today.plusDays(i),
                            15.0, 22.0, 35.0, 48.0, 65.0,
                            0.0, 0.0, 0.0
                    ));
                }
                try {
                    String json = objectMapper.writeValueAsString(calmDays);
                    forecastRepository.save(new ForecastSnapshotReplicaEntity(
                            loc.getLocationId(),
                            now,
                            false,
                            51,
                            json
                    ));
                } catch (Exception ex) {
                    log.error("Failed to seed initial forecast JSON for {}", loc.getLocationId(), ex);
                }
            }
        }
    }

    public void processFloodSignal(FloodSignalPayload payload) {
        String locationId = payload.locationId();
        Instant now = timeProvider.now();

        // 1. Ensure location replica exists or get location name
        MonitoredLocationReplicaEntity location = locationRepository.findById(locationId)
                .orElseGet(() -> new MonitoredLocationReplicaEntity(
                        locationId, locationId, "India", 0.0, 0.0, "River", 50.0, 100.0, 150.0, Language.en, true
                ));

        // 2. Cache forecast snapshot
        try {
            String daysJson = objectMapper.writeValueAsString(payload.days());
            forecastRepository.save(new ForecastSnapshotReplicaEntity(
                    locationId,
                    payload.dataAsOf(),
                    payload.isSimulated(),
                    payload.memberCount(),
                    daysJson
            ));
        } catch (Exception ex) {
            log.error("Failed to serialize forecast days for {}", locationId, ex);
        }

        // 3. Find or create AlertState
        AlertStateEntity state = alertStateRepository.findById(locationId)
                .orElse(new AlertStateEntity(
                        locationId,
                        location.getName(),
                        Hazard.FLOOD,
                        Severity.NONE,
                        Severity.NONE,
                        0,
                        null,
                        Provenance.MODEL_ONLY,
                        null,
                        0.0,
                        payload.dataAsOf(),
                        now,
                        null,
                        false
                ));

        // 4. Run Exceedance Calculation
        ExceedanceEvaluation eval = exceedanceCalculator.evaluate(payload.days());

        // 5. Run Severity State Machine
        Severity fromSeverity = state.getCurrentSeverity();
        StateTransition transition = stateMachine.evaluateTransition(
                state.getCurrentSeverity(),
                state.getCandidateSeverity(),
                state.getCandidateRuns(),
                eval.candidateSeverity(),
                payload.dataAsOf(),
                state.getLastNotificationAt(),
                now,
                payload.isSimulated()
        );

        // 6. Update Alert State
        state.setCurrentSeverity(transition.targetSeverity());
        state.setCandidateSeverity(transition.candidateSeverity());
        state.setCandidateRuns(transition.candidateRuns());
        state.setStale(transition.isStale());
        state.setDataAsOf(payload.dataAsOf());
        state.setFirstExceedanceDate(eval.firstExceedanceDate());
        state.setPeakExceedanceShare(eval.peakShare());

        String alertIdToEmit = state.getActiveAlertId();

        if (transition.severityChanged()) {
            state.setLastTransitionAt(now);

            if (transition.targetSeverity() == Severity.NONE) {
                // All-clear issued
                alertIdToEmit = (state.getActiveAlertId() != null) ? state.getActiveAlertId() : UUID.randomUUID().toString();
                state.setActiveAlertId(null);
            } else {
                if (state.getActiveAlertId() == null) {
                    state.setActiveAlertId(UUID.randomUUID().toString());
                }
                alertIdToEmit = state.getActiveAlertId();
            }

            // Save history record
            AlertHistoryEntity history = new AlertHistoryEntity(
                    alertIdToEmit,
                    locationId,
                    Hazard.FLOOD,
                    fromSeverity,
                    transition.targetSeverity(),
                    state.getProvenance(),
                    now,
                    eval.firstExceedanceDate(),
                    eval.peakShare(),
                    transition.reason()
            );
            alertHistoryRepository.save(history);
        }

        if (transition.shouldNotify()) {
            state.setLastNotificationAt(now);
        }

        alertStateRepository.save(state);

        // 7. Publish AlertEvent if severity changed or cooldown-based notification is due
        if (transition.severityChanged() || transition.shouldNotify()) {
            if (alertIdToEmit == null) {
                alertIdToEmit = UUID.randomUUID().toString();
            }
            AlertEventPayload eventPayload = new AlertEventPayload(
                    alertIdToEmit,
                    locationId,
                    Hazard.FLOOD,
                    "DECISION",
                    fromSeverity,
                    transition.targetSeverity(),
                    state.getProvenance(),
                    now,
                    eval.firstExceedanceDate(),
                    eval.peakShare(),
                    transition.shouldNotify()
            );
            eventPublisher.publish(eventPayload);
        }

        log.info("Processed FloodSignal for {}: candidate={}, runs={}, state {} -> {}, reason={}",
                locationId, eval.candidateSeverity(), transition.candidateRuns(),
                fromSeverity, transition.targetSeverity(), transition.reason());
    }

    public List<LocationAlertStatusResponse> getAllLocationStatuses() {
        Instant now = timeProvider.now();
        List<AlertStateEntity> states = alertStateRepository.findAll();
        List<LocationAlertStatusResponse> responses = new ArrayList<>();

        for (AlertStateEntity state : states) {
            long dataAgeSeconds = 0;
            if (state.getDataAsOf() != null) {
                dataAgeSeconds = Math.max(0, Duration.between(state.getDataAsOf(), now).toSeconds());
            }
            boolean isStale = state.isStale() || (dataAgeSeconds > 36 * 3600);

            responses.add(new LocationAlertStatusResponse(
                    state.getLocationId(),
                    state.getLocationName(),
                    state.getHazard(),
                    state.getCurrentSeverity(),
                    state.getProvenance(),
                    state.getActiveAlertId(),
                    state.getFirstExceedanceDate(),
                    state.getPeakExceedanceShare(),
                    state.getDataAsOf(),
                    dataAgeSeconds,
                    isStale,
                    state.getLastTransitionAt()
            ));
        }
        return responses;
    }

    public LocationForecastResponse getForecastForLocation(String locationId) {
        MonitoredLocationReplicaEntity loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown monitored location: " + locationId));

        Thresholds thresholds = new Thresholds(
                loc.getThreshold2yr(),
                loc.getThreshold5yr(),
                loc.getThreshold20yr()
        );

        ForecastSnapshotReplicaEntity snapshot = forecastRepository.findById(locationId)
                .orElse(null);

        List<ForecastDayStats> days = Collections.emptyList();
        if (snapshot != null && snapshot.getForecastDaysJson() != null) {
            try {
                days = objectMapper.readValue(
                        snapshot.getForecastDaysJson(),
                        new TypeReference<List<ForecastDayStats>>() {}
                );
            } catch (Exception ex) {
                log.error("Failed to parse cached forecast JSON for {}", locationId, ex);
            }
        }

        return new LocationForecastResponse(locationId, thresholds, days);
    }

    public void upsertLocationReplica(LocationUpsertedPayload payload) {
        MonitoredLocationReplicaEntity loc = locationRepository.findById(payload.locationId())
                .orElse(new MonitoredLocationReplicaEntity());
        loc.setLocationId(payload.locationId());
        loc.setName(payload.name());
        loc.setState(payload.state());
        loc.setLatitude(payload.latitude());
        loc.setLongitude(payload.longitude());
        loc.setRiverName(payload.riverName());
        loc.setThreshold2yr(payload.threshold2yr());
        loc.setThreshold5yr(payload.threshold5yr());
        loc.setThreshold20yr(payload.threshold20yr());
        loc.setPrimaryLanguage(payload.primaryLanguage());
        loc.setActive(payload.active());
        locationRepository.save(loc);

        // Ensure alert state exists
        if (!alertStateRepository.existsById(payload.locationId())) {
            Instant now = timeProvider.now();
            alertStateRepository.save(new AlertStateEntity(
                    payload.locationId(),
                    payload.name(),
                    Hazard.FLOOD,
                    Severity.NONE,
                    Severity.NONE,
                    0,
                    null,
                    Provenance.MODEL_ONLY,
                    null,
                    0.0,
                    now,
                    now,
                    null,
                    false
            ));
        }
        log.info("Upserted monitored location replica for {}", payload.locationId());
    }

    public void updateProvenance(String locationId, Provenance newProvenance) {
        AlertStateEntity state = alertStateRepository.findById(locationId).orElse(null);
        if (state != null) {
            state.setProvenance(newProvenance);
            alertStateRepository.save(state);
            log.info("Upgraded provenance for location {} to {}", locationId, newProvenance);
        }
    }
}
