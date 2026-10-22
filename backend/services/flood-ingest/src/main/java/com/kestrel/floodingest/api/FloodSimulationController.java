package com.kestrel.floodingest.api;

import com.kestrel.floodingest.api.dto.ForecastResponse;
import com.kestrel.floodingest.api.dto.ForecastResponse.ThresholdsDto;
import com.kestrel.floodingest.api.dto.SimulationRequest;
import com.kestrel.floodingest.api.dto.SimulationResponse;
import com.kestrel.floodingest.api.dto.SyncLiveResponse;
import com.kestrel.floodingest.app.FloodForecastService;
import com.kestrel.floodingest.app.FloodForecastService.LocationForecastView;
import com.kestrel.floodingest.app.FloodForecastService.SimulationResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flood")
public class FloodSimulationController {

    private final FloodForecastService forecastService;

    public FloodSimulationController(FloodForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<SimulationResponse> simulateScenario(
            @Valid @RequestBody SimulationRequest request) {
        SimulationResult result = forecastService.simulate(request.locationId(), request.scenario());
        return ResponseEntity.ok(new SimulationResponse(
                result.scenario(),
                result.locationId(),
                result.message()
        ));
    }

    @PostMapping("/sync-live")
    public ResponseEntity<SyncLiveResponse> syncLive() {
        int synced = forecastService.syncLiveAll();
        return ResponseEntity.ok(new SyncLiveResponse(
                "Live Open-Meteo flood data synced for all monitored locations.",
                synced
        ));
    }

    @GetMapping("/forecast/{locationId}")
    public ResponseEntity<ForecastResponse> getForecast(@PathVariable String locationId) {
        return forecastService.getLatestForecast(locationId)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ForecastResponse mapToResponse(LocationForecastView view) {
        return new ForecastResponse(
                view.locationId(),
                new ThresholdsDto(view.threshold2yr(), view.threshold5yr(), view.threshold20yr()),
                view.days()
        );
    }
}
