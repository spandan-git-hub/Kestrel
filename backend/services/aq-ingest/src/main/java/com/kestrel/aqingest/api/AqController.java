package com.kestrel.aqingest.api;

import com.kestrel.aqingest.app.AqForecastService;
import com.kestrel.aqingest.infra.entity.AqFeedStateEntity;
import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.repository.AqMonitoredPointRepository;
import com.kestrel.common.events.IndianRiverCatalog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/aq")
public class AqController {

    private final AqForecastService forecastService;
    private final AqMonitoredPointRepository pointRepository;

    public AqController(
            AqForecastService forecastService,
            AqMonitoredPointRepository pointRepository) {
        this.forecastService = forecastService;
        this.pointRepository = pointRepository;
    }

    /**
     * GET /api/v1/aq/points
     * Current 24h/8h pollutant averages and data age across all locations.
     */
    @GetMapping("/points")
    public ResponseEntity<List<AqPointSummary>> getAqPoints() {
        List<AqMonitoredPointEntity> points = pointRepository.findAll();
        if (points.isEmpty()) {
            points = IndianRiverCatalog.ALL_45_STATIONS.stream()
                    .map(st -> new AqMonitoredPointEntity(
                            st.locationId(),
                            st.name(),
                            st.state(),
                            st.latitude(),
                            st.longitude(),
                            st.active()
                    ))
                    .toList();
        }

        List<AqPointSummary> result = new ArrayList<>();
        Instant now = Instant.now();

        for (AqMonitoredPointEntity pt : points) {
            Optional<AqForecastRunEntity> runOpt = forecastService.getLatestRun(pt.getLocationId());
            Instant dataAsOf = runOpt.map(AqForecastRunEntity::getDataAsOf).orElse(null);
            Long ageHours = dataAsOf != null ? Duration.between(dataAsOf, now).toHours() : null;
            boolean isStale = ageHours != null && ageHours > 18;

            Double pm25_24h = runOpt.map(AqForecastRunEntity::getCurrentPm25_24h).orElse(null);
            Double pm10_24h = runOpt.map(AqForecastRunEntity::getCurrentPm10_24h).orElse(null);
            Double no2_24h = runOpt.map(AqForecastRunEntity::getCurrentNo2_24h).orElse(null);
            Double so2_24h = runOpt.map(AqForecastRunEntity::getCurrentSo2_24h).orElse(null);
            Double o3_8h = runOpt.map(AqForecastRunEntity::getCurrentO3_8h).orElse(null);
            Double co_8h_mgm3 = runOpt.map(AqForecastRunEntity::getCurrentCo_8h_mgm3).orElse(null);
            Integer validHours = runOpt.map(AqForecastRunEntity::getValidHours).orElse(null);

            result.add(new AqPointSummary(
                    pt.getLocationId(),
                    pt.getName(),
                    pt.getState(),
                    pt.getLatitude(),
                    pt.getLongitude(),
                    pm25_24h,
                    pm10_24h,
                    no2_24h,
                    so2_24h,
                    o3_8h,
                    co_8h_mgm3,
                    validHours,
                    dataAsOf,
                    ageHours,
                    isStale
            ));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/aq/points/{locationId}/forecast
     * Latest forecast run and hourly pollutant telemetry for a location.
     */
    @GetMapping("/points/{locationId}/forecast")
    public ResponseEntity<AqPointForecastResponse> getPointForecast(@PathVariable String locationId) {
        Optional<AqForecastRunEntity> runOpt = forecastService.getLatestRun(locationId);
        List<AqHourlyValueEntity> hourly = forecastService.getHourlyValues(locationId);

        if (runOpt.isEmpty() && hourly.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new AqPointForecastResponse(
                locationId,
                runOpt.orElse(null),
                hourly
        ));
    }

    /**
     * POST /api/v1/aq/sync-live?locationId={id}
     * Triggers live Open-Meteo AQ API sync for a location or all locations.
     */
    @PostMapping("/sync-live")
    public ResponseEntity<AqSyncResponse> syncLive(
            @RequestParam(value = "locationId", required = false) String locationId) {
        if (locationId != null && !locationId.isBlank()) {
            Optional<AqForecastService.AqSyncResult> resOpt = forecastService.syncLive(locationId);
            if (resOpt.isEmpty()) {
                return ResponseEntity.internalServerError().body(
                        new AqSyncResponse("Failed to fetch live Open-Meteo AQ telemetry for " + locationId, 0, false)
                );
            }
            return ResponseEntity.ok(new AqSyncResponse(
                    "Successfully synced AQ telemetry for " + locationId, 1, true
            ));
        } else {
            int count = forecastService.syncLiveAll();
            return ResponseEntity.ok(new AqSyncResponse(
                    "Successfully synced AQ telemetry across Indian locations", count, true
            ));
        }
    }

    @PostMapping("/sync-live/{locationId}")
    public ResponseEntity<AqSyncResponse> syncLivePath(@PathVariable String locationId) {
        return syncLive(locationId);
    }

    /**
     * GET /api/v1/aq/feed-status
     * Reports live feed health of Open-Meteo AQ.
     */
    @GetMapping("/feed-status")
    public ResponseEntity<AqFeedStatusResponse> getFeedStatus() {
        Optional<AqFeedStateEntity> stateOpt = forecastService.getFeedStatus();
        if (stateOpt.isEmpty()) {
            return ResponseEntity.ok(new AqFeedStatusResponse("OPEN_METEO_AQ", "HEALTHY", null, null, 0));
        }
        AqFeedStateEntity s = stateOpt.get();
        return ResponseEntity.ok(new AqFeedStatusResponse(
                s.getFeedName(),
                s.getStatus(),
                s.getLastSuccessAt(),
                s.getLastFailureAt(),
                s.getConsecutiveFailures()
        ));
    }

    public record AqPointSummary(
            String locationId,
            String name,
            String state,
            double latitude,
            double longitude,
            Double pm25_24h,
            Double pm10_24h,
            Double no2_24h,
            Double so2_24h,
            Double o3_8h,
            Double co_8h_mgm3,
            Integer validHours,
            Instant dataAsOf,
            Long ageHours,
            boolean isStale
    ) {}

    public record AqPointForecastResponse(
            String locationId,
            AqForecastRunEntity latestRun,
            List<AqHourlyValueEntity> hourlyValues
    ) {}

    public record AqSyncResponse(
            String message,
            int syncedStationsCount,
            boolean success
    ) {}

    public record AqFeedStatusResponse(
            String feedName,
            String status,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            int consecutiveFailures
    ) {}
}

