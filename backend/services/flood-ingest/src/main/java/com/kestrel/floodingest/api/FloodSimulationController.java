package com.kestrel.floodingest.api;

import com.kestrel.floodingest.api.dto.ForecastResponse;
import com.kestrel.floodingest.api.dto.ForecastResponse.ThresholdsDto;
import com.kestrel.floodingest.api.dto.SyncLiveResponse;
import com.kestrel.floodingest.app.FloodForecastService;
import com.kestrel.floodingest.app.FloodForecastService.LocationForecastView;
import com.kestrel.floodingest.app.FloodForecastService.SyncResult;
import com.kestrel.floodingest.app.GumbelThresholdFittingService;
import com.kestrel.floodingest.app.GumbelThresholdFittingService.ThresholdFitResult;
import com.kestrel.floodingest.infra.entity.FloodThresholdEntity;
import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/flood")
public class FloodSimulationController {

    private final FloodForecastService forecastService;
    private final GumbelThresholdFittingService thresholdFittingService;

    public FloodSimulationController(
            FloodForecastService forecastService,
            GumbelThresholdFittingService thresholdFittingService) {
        this.forecastService = forecastService;
        this.thresholdFittingService = thresholdFittingService;
    }

    @PostMapping({"/sync-live", "/sync-live-all"})
    public ResponseEntity<SyncLiveResponse> syncLiveAll() {
        int synced = forecastService.syncLiveAll();
        return ResponseEntity.ok(new SyncLiveResponse(
                "Live Open-Meteo flood data synced for all monitored locations.",
                synced
        ));
    }

    @PostMapping("/sync-live/{locationId}")
    public ResponseEntity<SyncLiveLocationResponse> syncLiveLocation(@PathVariable String locationId) {
        SyncResult result = forecastService.syncLive(locationId);
        return ResponseEntity.ok(new SyncLiveLocationResponse(
                result.locationId(),
                result.memberCount(),
                result.success(),
                result.message()
        ));
    }

    @PostMapping("/thresholds/fit/{locationId}")
    public ResponseEntity<ThresholdFitResult> fitThresholds(@PathVariable String locationId) {
        ThresholdFitResult result = thresholdFittingService.fitThresholdsForLocation(locationId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/thresholds/fit-all")
    public ResponseEntity<Map<String, ThresholdFitResult>> fitAllThresholds() {
        Map<String, ThresholdFitResult> results = thresholdFittingService.fitThresholdsForAllLocations();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/thresholds/{locationId}")
    public ResponseEntity<FloodThresholdEntity> getThreshold(@PathVariable String locationId) {
        return thresholdFittingService.getThreshold(locationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/thresholds")
    public ResponseEntity<List<FloodThresholdEntity>> getAllThresholds() {
        return ResponseEntity.ok(thresholdFittingService.getAllThresholds());
    }

    @GetMapping("/forecast/{locationId}")
    public ResponseEntity<ForecastResponse> getForecast(@PathVariable String locationId) {
        return forecastService.getLatestForecast(locationId)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/locations")
    public ResponseEntity<List<MonitoredPointReplica>> getAllLocations() {
        return ResponseEntity.ok(forecastService.getAllMonitoredLocations());
    }

    private ForecastResponse mapToResponse(LocationForecastView view) {
        return new ForecastResponse(
                view.locationId(),
                new ThresholdsDto(view.threshold2yr(), view.threshold5yr(), view.threshold20yr()),
                view.days()
        );
    }

    public record SyncLiveLocationResponse(
            String locationId,
            int memberCount,
            boolean success,
            String message
    ) {}
}
