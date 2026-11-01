package com.kestrel.aqingest.api;

import com.kestrel.aqingest.app.AqForecastService;
import com.kestrel.aqingest.infra.entity.AqFeedStateEntity;
import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.repository.AqMonitoredPointRepository;
import com.kestrel.common.core.aqi.AqiCalculator;
import com.kestrel.common.core.aqi.InsufficientDataException;
import com.kestrel.common.events.AqiCategory;
import com.kestrel.common.events.AqSignalPayload;
import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.Pollutant;
import com.kestrel.common.events.Severity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/aq")
public class AqController {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

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
     * Current 24h/8h pollutant averages, overall CPCB AQI, sub-indices, and data age across all locations.
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

            Integer aqi = null;
            AqiCategory category = null;
            Pollutant dominantPollutant = null;
            Severity severity = Severity.NONE;
            Map<Pollutant, Integer> subIndices = new EnumMap<>(Pollutant.class);

            if (runOpt.isPresent()) {
                Map<Pollutant, Double> concMap = new EnumMap<>(Pollutant.class);
                if (pm25_24h != null) concMap.put(Pollutant.PM25, pm25_24h);
                if (pm10_24h != null) concMap.put(Pollutant.PM10, pm10_24h);
                if (no2_24h != null) concMap.put(Pollutant.NO2, no2_24h);
                if (so2_24h != null) concMap.put(Pollutant.SO2, so2_24h);
                if (o3_8h != null) concMap.put(Pollutant.O3, o3_8h);
                if (co_8h_mgm3 != null) concMap.put(Pollutant.CO, co_8h_mgm3);

                for (Map.Entry<Pollutant, Double> e : concMap.entrySet()) {
                    subIndices.put(e.getKey(), AqiCalculator.calculateSubIndex(e.getKey(), e.getValue()));
                }

                try {
                    AqiCalculator.AqiResult detailed = AqiCalculator.calculateDetailedAqi(concMap);
                    aqi = detailed.aqi();
                    category = detailed.category();
                    dominantPollutant = detailed.dominantPollutant();
                    severity = AqiCalculator.toSeverity(category);
                    subIndices = detailed.subIndices();
                } catch (InsufficientDataException ignored) {
                    // CPCB minimum criteria not yet satisfied
                }
            }

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
                    aqi,
                    category,
                    dominantPollutant,
                    severity,
                    subIndices,
                    dataAsOf,
                    ageHours,
                    isStale
            ));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/aq/points/{locationId}/forecast
     * 3-day peak pollutant forecast and projected AQI categories.
     */
    @GetMapping("/points/{locationId}/forecast")
    public ResponseEntity<AqForecastDetails> getPointForecast(@PathVariable String locationId) {
        Optional<AqForecastRunEntity> runOpt = forecastService.getLatestRun(locationId);
        List<AqHourlyValueEntity> hourly = forecastService.getHourlyValues(locationId);

        if (runOpt.isEmpty() && hourly.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Instant dataAsOf = runOpt.map(AqForecastRunEntity::getDataAsOf).orElse(Instant.now());
        long ageHours = Duration.between(dataAsOf, Instant.now()).toHours();
        boolean isStale = ageHours > 18;

        List<AqForecastDayItem> dayItems = new ArrayList<>();
        List<AqSignalPayload.ForecastDayPeak> forecastPeaks = forecastService.getForecastPeaks(locationId);

        for (AqSignalPayload.ForecastDayPeak peak : forecastPeaks) {
            Map<Pollutant, Double> peakMap = new EnumMap<>(Pollutant.class);
            peakMap.put(Pollutant.PM25, peak.pm25_24h());
            peakMap.put(Pollutant.PM10, peak.pm10_24h());
            peakMap.put(Pollutant.NO2, peak.no2_24h());
            peakMap.put(Pollutant.SO2, peak.so2_24h());
            peakMap.put(Pollutant.O3, peak.o3_8h());
            peakMap.put(Pollutant.CO, peak.co_8h_mgm3());

            Map<Pollutant, Integer> subIndices = new EnumMap<>(Pollutant.class);
            for (Map.Entry<Pollutant, Double> e : peakMap.entrySet()) {
                subIndices.put(e.getKey(), AqiCalculator.calculateSubIndex(e.getKey(), e.getValue()));
            }

            Integer projectedAqi = null;
            AqiCategory projectedCategory = null;
            Pollutant dominant = null;
            Severity projSeverity = Severity.NONE;

            try {
                AqiCalculator.AqiResult res = AqiCalculator.calculateDetailedAqi(peakMap);
                projectedAqi = res.aqi();
                projectedCategory = res.category();
                dominant = res.dominantPollutant();
                projSeverity = AqiCalculator.toSeverity(projectedCategory);
                subIndices = res.subIndices();
            } catch (InsufficientDataException ignored) {}

            dayItems.add(new AqForecastDayItem(
                    peak.date().toString(),
                    peak.pm25_24h(),
                    peak.pm10_24h(),
                    peak.no2_24h(),
                    peak.so2_24h(),
                    peak.o3_8h(),
                    peak.co_8h_mgm3(),
                    projectedAqi,
                    projectedCategory,
                    dominant,
                    projSeverity,
                    subIndices
            ));
        }

        // Fallback: If no forecast peaks extracted but run exists, build current day item
        if (dayItems.isEmpty() && runOpt.isPresent()) {
            AqForecastRunEntity run = runOpt.get();
            Map<Pollutant, Double> currentMap = new EnumMap<>(Pollutant.class);
            currentMap.put(Pollutant.PM25, run.getCurrentPm25_24h());
            currentMap.put(Pollutant.PM10, run.getCurrentPm10_24h());
            currentMap.put(Pollutant.NO2, run.getCurrentNo2_24h());
            currentMap.put(Pollutant.SO2, run.getCurrentSo2_24h());
            currentMap.put(Pollutant.O3, run.getCurrentO3_8h());
            currentMap.put(Pollutant.CO, run.getCurrentCo_8h_mgm3());

            Map<Pollutant, Integer> subIndices = new EnumMap<>(Pollutant.class);
            for (Map.Entry<Pollutant, Double> e : currentMap.entrySet()) {
                subIndices.put(e.getKey(), AqiCalculator.calculateSubIndex(e.getKey(), e.getValue()));
            }

            Integer aqi = null;
            AqiCategory category = null;
            Pollutant dominant = null;
            Severity severity = Severity.NONE;

            try {
                AqiCalculator.AqiResult res = AqiCalculator.calculateDetailedAqi(currentMap);
                aqi = res.aqi();
                category = res.category();
                dominant = res.dominantPollutant();
                severity = AqiCalculator.toSeverity(category);
                subIndices = res.subIndices();
            } catch (InsufficientDataException ignored) {}

            dayItems.add(new AqForecastDayItem(
                    LocalDate.ofInstant(dataAsOf, IST_ZONE).toString(),
                    run.getCurrentPm25_24h(),
                    run.getCurrentPm10_24h(),
                    run.getCurrentNo2_24h(),
                    run.getCurrentSo2_24h(),
                    run.getCurrentO3_8h(),
                    run.getCurrentCo_8h_mgm3(),
                    aqi,
                    category,
                    dominant,
                    severity,
                    subIndices
            ));
        }

        return ResponseEntity.ok(new AqForecastDetails(
                locationId,
                dataAsOf,
                ageHours,
                isStale,
                runOpt.orElse(null),
                dayItems,
                hourly
        ));
    }

    /**
     * POST /api/v1/aq/sync-live?locationId={id} or POST /api/v1/aq/sync-live/{locationId}
     * On-demand live AQ sync for a location or all locations.
     */
    @PostMapping(value = {"/sync-live", "/sync-live/{locationId}"})
    public ResponseEntity<AqSyncResponse> syncLive(
            @PathVariable(required = false) String locationId,
            @RequestParam(name = "locationId", required = false) String paramLocationId) {
        String locId = (locationId != null && !locationId.isBlank()) ? locationId : paramLocationId;
        if (locId != null && !locId.isBlank()) {
            Optional<AqForecastService.AqSyncResult> resOpt = forecastService.syncLive(locId);
            if (resOpt.isPresent()) {
                return ResponseEntity.ok(new AqSyncResponse(
                        "Successfully synchronized AQ telemetry for " + locId,
                        1,
                        List.of(locId),
                        true
                ));
            } else {
                return ResponseEntity.badRequest().body(new AqSyncResponse(
                        "Failed to sync AQ telemetry for location " + locId,
                        0,
                        List.of(),
                        false
                ));
            }
        }

        int synced = forecastService.syncLiveAll();
        return ResponseEntity.ok(new AqSyncResponse(
                "Successfully synchronized AQ telemetry across Indian locations",
                synced,
                List.of(),
                true
        ));
    }

    /**
     * GET /api/v1/aq/feed-status
     * Reports live feed health of Open-Meteo AQ.
     */
    @GetMapping("/feed-status")
    public ResponseEntity<AqFeedStatusResponse> getFeedStatus() {
        Optional<AqFeedStateEntity> stateOpt = forecastService.getFeedStatus();
        if (stateOpt.isEmpty()) {
            return ResponseEntity.ok(new AqFeedStatusResponse("OPEN_METEO_AQ", "HEALTHY", Instant.now(), null, 0));
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
            Integer aqi,
            AqiCategory category,
            Pollutant dominantPollutant,
            Severity severity,
            Map<Pollutant, Integer> subIndices,
            Instant dataAsOf,
            Long ageHours,
            boolean isStale
    ) {}

    public record AqForecastDayItem(
            String date,
            double peakPm25_24h,
            double peakPm10_24h,
            double peakNo2_24h,
            double peakSo2_24h,
            double peakO3_8h,
            double peakCo_8h_mgm3,
            Integer projectedAqi,
            AqiCategory projectedCategory,
            Pollutant dominantPollutant,
            Severity projectedSeverity,
            Map<Pollutant, Integer> subIndices
    ) {}

    public record AqForecastDetails(
            String locationId,
            Instant dataAsOf,
            long ageHours,
            boolean isStale,
            AqForecastRunEntity latestRun,
            List<AqForecastDayItem> days,
            List<AqHourlyValueEntity> hourlyValues
    ) {}

    public record AqSyncResponse(
            String message,
            int syncedCount,
            List<String> locations,
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
