package com.kestrel.heatingest.api;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.RegionType;
import com.kestrel.heatingest.app.HeatForecastService;
import com.kestrel.heatingest.infra.entity.HeatFeedStateEntity;
import com.kestrel.heatingest.infra.entity.HeatForecastDayEntity;
import com.kestrel.heatingest.infra.entity.HeatForecastRunEntity;
import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;
import com.kestrel.heatingest.infra.repository.HeatMonitoredPointRepository;
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
@RequestMapping("/api/v1/heat")
public class HeatController {

    private final HeatForecastService forecastService;
    private final HeatMonitoredPointRepository pointRepository;

    public HeatController(HeatForecastService forecastService, HeatMonitoredPointRepository pointRepository) {
        this.forecastService = forecastService;
        this.pointRepository = pointRepository;
    }

    /**
     * GET /api/v1/heat/points
     * Current temperature, apparent temperature, normal departure, and data age across all locations.
     */
    @GetMapping("/points")
    public ResponseEntity<List<HeatPointSummary>> getHeatPoints() {
        List<HeatMonitoredPointEntity> points = pointRepository.findAll();
        if (points.isEmpty()) {
            // Fallback from catalog
            points = IndianRiverCatalog.ALL_45_STATIONS.stream()
                    .map(st -> new HeatMonitoredPointEntity(
                            st.locationId(),
                            st.name(),
                            st.state(),
                            st.latitude(),
                            st.longitude(),
                            IndianRiverCatalog.getRegionType(st.locationId()),
                            st.active()
                    ))
                    .toList();
        }

        List<HeatPointSummary> result = new ArrayList<>();
        Instant now = Instant.now();

        for (HeatMonitoredPointEntity pt : points) {
            List<HeatForecastDayEntity> days = forecastService.getForecastDays(pt.getLocationId());
            Optional<HeatForecastRunEntity> runOpt = forecastService.getLatestRun(pt.getLocationId());

            Double currentTmax = null;
            Double currentApparentMax = null;
            Double normalTmax = null;
            Double departure = null;
            Instant dataAsOf = runOpt.map(HeatForecastRunEntity::getDataAsOf).orElse(null);
            Long ageHours = dataAsOf != null ? Duration.between(dataAsOf, now).toHours() : null;
            boolean isStale = ageHours != null && ageHours > 18;

            if (!days.isEmpty()) {
                HeatForecastDayEntity firstDay = days.get(0);
                currentTmax = firstDay.getTmax();
                currentApparentMax = firstDay.getApparentMax();
                normalTmax = firstDay.getNormalTmax();
                departure = firstDay.getDeparture();
            }

            result.add(new HeatPointSummary(
                    pt.getLocationId(),
                    pt.getName(),
                    pt.getState(),
                    pt.getLatitude(),
                    pt.getLongitude(),
                    pt.getRegionType(),
                    currentTmax,
                    currentApparentMax,
                    normalTmax,
                    departure,
                    dataAsOf,
                    ageHours,
                    isStale
            ));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/heat/points/{locationId}/forecast
     * 7-day temperature forecast with normal departures and IMD heat-wave criteria flags.
     */
    @GetMapping("/points/{locationId}/forecast")
    public ResponseEntity<HeatForecastDetails> getPointForecast(@PathVariable String locationId) {
        List<HeatForecastDayEntity> days = forecastService.getForecastDays(locationId);
        Optional<HeatForecastRunEntity> runOpt = forecastService.getLatestRun(locationId);
        RegionType regionType = IndianRiverCatalog.getRegionType(locationId);

        if (days.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Instant dataAsOf = runOpt.map(HeatForecastRunEntity::getDataAsOf).orElse(Instant.now());
        long ageHours = Duration.between(dataAsOf, Instant.now()).toHours();
        boolean isStale = ageHours > 18;

        List<HeatForecastDayItem> dayItems = new ArrayList<>();
        for (HeatForecastDayEntity d : days) {
            boolean gateMet = isGateMet(regionType, d.getTmax());
            boolean isHeatWave = gateMet && (d.getDeparture() >= 4.5 || d.getTmax() >= 45.0);
            boolean isSevereHeatWave = gateMet && (d.getDeparture() > 6.4 || d.getTmax() >= 47.0);

            dayItems.add(new HeatForecastDayItem(
                    d.getDate(),
                    d.getTmax(),
                    d.getApparentMax(),
                    d.getHumidityMax(),
                    d.getNormalTmax(),
                    d.getDeparture(),
                    gateMet,
                    isHeatWave,
                    isSevereHeatWave
            ));
        }

        return ResponseEntity.ok(new HeatForecastDetails(
                locationId,
                regionType,
                dataAsOf,
                ageHours,
                isStale,
                dayItems
        ));
    }

    /**
     * POST /api/v1/heat/sync-live?locationId={id}
     * On-demand live heat sync for a location or all locations.
     */
    @PostMapping("/sync-live")
    public ResponseEntity<HeatSyncResponse> syncLive(@RequestParam(required = false) String locationId) {
        if (locationId != null && !locationId.isBlank()) {
            Optional<HeatForecastService.HeatSyncResult> resultOpt = forecastService.syncLive(locationId);
            if (resultOpt.isPresent()) {
                HeatForecastService.HeatSyncResult res = resultOpt.get();
                return ResponseEntity.ok(new HeatSyncResponse(
                        res.message(),
                        1,
                        List.of(locationId),
                        true
                ));
            } else {
                return ResponseEntity.badRequest().body(new HeatSyncResponse(
                        "Failed to sync weather forecast for location " + locationId,
                        0,
                        List.of(),
                        false
                ));
            }
        }

        int synced = forecastService.syncLiveAll();
        return ResponseEntity.ok(new HeatSyncResponse(
                "Successfully synchronized heat forecast for " + synced + " stations.",
                synced,
                List.of(),
                true
        ));
    }

    /**
     * GET /api/v1/heat/feed-status
     * Live feed status and failure tracking.
     */
    @GetMapping("/feed-status")
    public ResponseEntity<HeatFeedStatusResponse> getFeedStatus() {
        Optional<HeatFeedStateEntity> stateOpt = forecastService.getFeedStatus();
        if (stateOpt.isPresent()) {
            HeatFeedStateEntity s = stateOpt.get();
            return ResponseEntity.ok(new HeatFeedStatusResponse(
                    s.getFeedName(),
                    s.getStatus(),
                    s.getLastSuccessAt(),
                    s.getLastFailureAt(),
                    s.getConsecutiveFailures()
            ));
        }

        return ResponseEntity.ok(new HeatFeedStatusResponse(
                "OPEN_METEO_WEATHER",
                "HEALTHY",
                Instant.now(),
                null,
                0
        ));
    }

    private boolean isGateMet(RegionType regionType, double tmax) {
        return switch (regionType) {
            case PLAINS -> tmax >= 40.0;
            case COASTAL -> tmax >= 37.0;
            case HILLY -> tmax >= 30.0;
        };
    }

    public record HeatPointSummary(
            String locationId,
            String name,
            String state,
            double latitude,
            double longitude,
            RegionType regionType,
            Double currentTmax,
            Double currentApparentMax,
            Double normalTmax,
            Double departure,
            Instant dataAsOf,
            Long ageHours,
            boolean isStale
    ) {}

    public record HeatForecastDayItem(
            String date,
            double tmax,
            double apparentMax,
            double humidityMax,
            double normalTmax,
            double departure,
            boolean regionGateMet,
            boolean isHeatWaveDay,
            boolean isSevereHeatWaveDay
    ) {}

    public record HeatForecastDetails(
            String locationId,
            RegionType regionType,
            Instant dataAsOf,
            long ageHours,
            boolean isStale,
            List<HeatForecastDayItem> days
    ) {}

    public record HeatSyncResponse(
            String message,
            int syncedCount,
            List<String> locations,
            boolean success
    ) {}

    public record HeatFeedStatusResponse(
            String feedName,
            String status,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            int consecutiveFailures
    ) {}
}

