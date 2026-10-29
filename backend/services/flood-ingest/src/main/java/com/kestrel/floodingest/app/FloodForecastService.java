package com.kestrel.floodingest.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.LocationUpsertedPayload;
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

import java.time.Instant;
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
    public void seedInitialLocationsIfEmpty() {
        if (replicaRepository.count() < IndianRiverCatalog.ALL_45_STATIONS.size()) {
            log.info("Seeding full catalog of {} Indian river stations in MonitoredPointReplica table",
                    IndianRiverCatalog.ALL_45_STATIONS.size());
            List<MonitoredPointReplica> seeds = IndianRiverCatalog.ALL_45_STATIONS.stream()
                    .map(s -> new MonitoredPointReplica(
                            s.locationId(),
                            s.name(),
                            s.state(),
                            s.latitude(),
                            s.longitude(),
                            s.riverName(),
                            s.basin(),
                            s.threshold2yr(),
                            s.threshold5yr(),
                            s.threshold20yr(),
                            s.primaryLanguage(),
                            s.active()
                    ))
                    .toList();
            replicaRepository.saveAll(seeds);
            log.info("Finished seeding {} Indian river stations in flood-ingest", seeds.size());
        }
    }

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
        IndianRiverCatalog.findById(payload.locationId()).ifPresent(s -> replica.setBasin(s.basin()));
        replicaRepository.save(replica);
        log.info("Upserted monitored point replica for {}", payload.locationId());
    }

    public SyncResult syncLive(String locationId) {
        MonitoredPointReplica location = replicaRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown monitored location: " + locationId));

        List<ForecastDayStats> days;
        int memberCount;
        boolean isStale = false;
        String message;

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
            log.warn("Open-Meteo live pull failed for {}. Checking cached snapshot.", locationId);
            Optional<FloodSnapshotEntity> cachedOpt = snapshotRepository.findFirstByLocationIdOrderByDataAsOfDesc(locationId);
            if (cachedOpt.isPresent()) {
                days = deserializeDays(cachedOpt.get().getDaysJson());
                memberCount = cachedOpt.get().getMemberCount();
                isStale = true;
                message = "Open-Meteo live pull failed; serving cached snapshot (flagged as stale).";
            } else {
                days = Collections.emptyList();
                memberCount = 0;
                isStale = true;
                message = "Open-Meteo live pull failed; no cached snapshot available.";
            }
        }

        if (!days.isEmpty()) {
            String daysJson = serializeDays(days);
            FloodSnapshotEntity snapshot = new FloodSnapshotEntity(
                    locationId,
                    Instant.now(),
                    false,
                    "LIVE_OPEN_METEO",
                    memberCount,
                    daysJson,
                    isStale
            );
            snapshotRepository.save(snapshot);

            FloodSignalPayload payload = new FloodSignalPayload(
                    locationId,
                    snapshot.getDataAsOf(),
                    false,
                    memberCount,
                    days
            );
            eventPublisher.publish(payload);
        }

        return new SyncResult(locationId, memberCount, !isStale && !days.isEmpty(), message);
    }

    public int syncLiveAll() {
        List<MonitoredPointReplica> activeLocations = replicaRepository.findByActiveTrue();
        int count = 0;
        for (MonitoredPointReplica loc : activeLocations) {
            try {
                SyncResult res = syncLive(loc.getLocationId());
                if (res.success()) {
                    count++;
                }
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
            // Pull live directly from Open-Meteo
            Optional<OpenMeteoForecastResult> liveResult = openMeteoClient.fetchEnsembleForecast(
                    loc.getLatitude(),
                    loc.getLongitude(),
                    loc.getThreshold2yr(),
                    loc.getThreshold5yr(),
                    loc.getThreshold20yr()
            );
            if (liveResult.isPresent() && !liveResult.get().dayStats().isEmpty()) {
                days = liveResult.get().dayStats();
                String daysJson = serializeDays(days);
                snapshotRepository.save(new FloodSnapshotEntity(
                        locationId,
                        Instant.now(),
                        false,
                        "LIVE_OPEN_METEO",
                        liveResult.get().memberCount(),
                        daysJson,
                        false
                ));
            } else {
                days = Collections.emptyList();
            }
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

    public record SyncResult(String locationId, int memberCount, boolean success, String message) {}

    public record LocationForecastView(
            String locationId,
            String locationName,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            List<ForecastDayStats> days
    ) {}
}
