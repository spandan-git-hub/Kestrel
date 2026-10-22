package com.kestrel.floodingest.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.SimulationScenario;
import com.kestrel.floodingest.domain.ScenarioSimulator;
import com.kestrel.floodingest.infra.client.OpenMeteoClient;
import com.kestrel.floodingest.infra.client.OpenMeteoClient.OpenMeteoForecastResult;
import com.kestrel.floodingest.infra.entity.FloodSnapshotEntity;
import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import com.kestrel.floodingest.infra.publisher.FloodSignalEventPublisher;
import com.kestrel.floodingest.infra.repository.FloodSnapshotRepository;
import com.kestrel.floodingest.infra.repository.MonitoredPointReplicaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class FloodForecastService {

    private static final Logger log = LoggerFactory.getLogger(FloodForecastService.class);

    private final MonitoredPointReplicaRepository replicaRepository;
    private final FloodSnapshotRepository snapshotRepository;
    private final OpenMeteoClient openMeteoClient;
    private final FloodSignalEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public FloodForecastService(
            MonitoredPointReplicaRepository replicaRepository,
            FloodSnapshotRepository snapshotRepository,
            OpenMeteoClient openMeteoClient,
            FloodSignalEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.replicaRepository = replicaRepository;
        this.snapshotRepository = snapshotRepository;
        this.openMeteoClient = openMeteoClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    @Transactional
    public void seedInitialLocationsIfEmpty() {
        if (replicaRepository.count() == 0) {
            log.info("Seeding initial 4 Indian pilot locations in MonitoredPointReplica table");
            List<MonitoredPointReplica> seeds = List.of(
                    new MonitoredPointReplica("mumbai-kurla", "Kurla Ward L, Mumbai", "Maharashtra",
                            19.0726, 72.8797, "Mithi River", 45.0, 85.0, 140.0, Language.mr, true),
                    new MonitoredPointReplica("patna-ward12", "Ward 12, Patna", "Bihar",
                            25.6127, 85.1589, "Ganges River", 38000.0, 48000.0, 62000.0, Language.hi, true),
                    new MonitoredPointReplica("guwahati-river", "Riverside, Guwahati", "Assam",
                            26.1856, 91.7478, "Brahmaputra River", 32000.0, 42000.0, 55000.0, Language.en, true),
                    new MonitoredPointReplica("cuttack-delta", "Cuttack Municipal Corp", "Odisha",
                            20.4625, 85.8830, "Mahanadi River", 18000.0, 26000.0, 35000.0, Language.en, true)
            );
            replicaRepository.saveAll(seeds);
        }
    }

    @Transactional
    public void upsertLocationReplica(LocationUpsertedPayload payload) {
        MonitoredPointReplica replica = replicaRepository.findById(payload.locationId())
                .orElse(new MonitoredPointReplica());
        replica.setLocationId(payload.locationId());
        replica.setName(payload.name());
        replica.setState(payload.state());
        replica.setLatitude(payload.latitude());
        replica.setLongitude(payload.longitude());
        replica.setRiverName(payload.riverName());
        replica.setThreshold2yr(payload.threshold2yr());
        replica.setThreshold5yr(payload.threshold5yr());
        replica.setThreshold20yr(payload.threshold20yr());
        replica.setPrimaryLanguage(payload.primaryLanguage());
        replica.setActive(payload.active());
        replicaRepository.save(replica);
        log.info("Upserted monitored point replica for {}", payload.locationId());
    }

    @Transactional
    public SimulationResult simulate(String locationId, SimulationScenario scenario) {
        MonitoredPointReplica location = replicaRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown monitored location: " + locationId));

        List<ForecastDayStats> days;
        int memberCount;
        boolean isSimulated = (scenario != SimulationScenario.LIVE_OPEN_METEO);
        boolean isStale = false;
        String message;

        if (scenario == SimulationScenario.LIVE_OPEN_METEO) {
            Optional<OpenMeteoForecastResult> liveResult = openMeteoClient.fetchEnsembleForecast(
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getThreshold2yr(),
                    location.getThreshold5yr(),
                    location.getThreshold20yr()
            );

            if (liveResult.isPresent() && !liveResult.get().dayStats().isEmpty()) {
                days = liveResult.get().dayStats();
                memberCount = liveResult.get().memberCount();
                message = "Live Open-Meteo ensemble forecast successfully synced (" + memberCount + " members).";
            } else {
                log.warn("Open-Meteo live pull failed for {}. Falling back to cached snapshot or calm baseline", locationId);
                Optional<FloodSnapshotEntity> cachedOpt = snapshotRepository.findFirstByLocationIdOrderByDataAsOfDesc(locationId);
                if (cachedOpt.isPresent()) {
                    days = deserializeDays(cachedOpt.get().getDaysJson());
                    memberCount = cachedOpt.get().getMemberCount();
                    isStale = true;
                    message = "Open-Meteo live pull failed; serving cached forecast (flagged as stale).";
                } else {
                    days = ScenarioSimulator.generateScenario(
                            SimulationScenario.CALM,
                            LocalDate.now(),
                            location.getThreshold2yr(),
                            location.getThreshold5yr(),
                            location.getThreshold20yr()
                    );
                    memberCount = 50;
                    isStale = true;
                    message = "Open-Meteo live pull failed; serving calm baseline (flagged as stale).";
                }
            }
        } else {
            days = ScenarioSimulator.generateScenario(
                    scenario,
                    LocalDate.now(),
                    location.getThreshold2yr(),
                    location.getThreshold5yr(),
                    location.getThreshold20yr()
            );
            memberCount = 50;
            message = switch (scenario) {
                case CALM -> "Injected calm hydrological baseline: 0% ensemble exceedance.";
                case WATCH_2YR -> "Injected synthetic 2-year flood watch: 42% ensemble members exceeding 2-yr threshold.";
                case WARNING_5YR -> "Injected synthetic 5-year flood spike: 68% ensemble members exceeding 5-yr threshold.";
                case SEVERE_20YR -> "Injected synthetic 20-year severe flood: 78% ensemble members exceeding 20-yr threshold.";
                default -> "Injected simulation scenario " + scenario;
            };
        }

        // Persist snapshot to isolated H2
        String daysJson = serializeDays(days);
        FloodSnapshotEntity snapshot = new FloodSnapshotEntity(
                locationId,
                Instant.now(),
                isSimulated,
                scenario.name(),
                memberCount,
                daysJson,
                isStale
        );
        snapshotRepository.save(snapshot);

        // Publish FloodSignalPayload to Event Bus
        FloodSignalPayload payload = new FloodSignalPayload(
                locationId,
                snapshot.getDataAsOf(),
                isSimulated,
                memberCount,
                days
        );
        eventPublisher.publish(payload);

        return new SimulationResult(scenario, locationId, message);
    }

    @Transactional
    public int syncLiveAll() {
        List<MonitoredPointReplica> activeLocations = replicaRepository.findByActiveTrue();
        int count = 0;
        for (MonitoredPointReplica loc : activeLocations) {
            try {
                simulate(loc.getLocationId(), SimulationScenario.LIVE_OPEN_METEO);
                count++;
            } catch (Exception ex) {
                log.error("Failed to sync live forecast for location {}: {}", loc.getLocationId(), ex.getMessage());
            }
        }
        return count;
    }

    public Optional<LocationForecastView> getLatestForecast(String locationId) {
        Optional<MonitoredPointReplica> locOpt = replicaRepository.findById(locationId);
        if (locOpt.isEmpty()) {
            return Optional.empty();
        }
        MonitoredPointReplica loc = locOpt.get();

        Optional<FloodSnapshotEntity> snapshotOpt = snapshotRepository.findFirstByLocationIdOrderByDataAsOfDesc(locationId);
        List<ForecastDayStats> days;
        if (snapshotOpt.isPresent()) {
            days = deserializeDays(snapshotOpt.get().getDaysJson());
        } else {
            days = ScenarioSimulator.generateScenario(
                    SimulationScenario.CALM,
                    LocalDate.now(),
                    loc.getThreshold2yr(),
                    loc.getThreshold5yr(),
                    loc.getThreshold20yr()
            );
        }

        return Optional.of(new LocationForecastView(
                locationId,
                loc.getName(),
                loc.getThreshold2yr(),
                loc.getThreshold5yr(),
                loc.getThreshold20yr(),
                days
        ));
    }

    public List<MonitoredPointReplica> getAllMonitoredLocations() {
        return replicaRepository.findAll();
    }

    private String serializeDays(List<ForecastDayStats> days) {
        try {
            return objectMapper.writeValueAsString(days);
        } catch (Exception e) {
            log.error("Failed to serialize days: {}", e.getMessage());
            return "[]";
        }
    }

    private List<ForecastDayStats> deserializeDays(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ForecastDayStats>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize days JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public record SimulationResult(SimulationScenario scenario, String locationId, String message) {}

    public record LocationForecastView(
            String locationId,
            String locationName,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            List<ForecastDayStats> days
    ) {}
}
