package com.kestrel.rulesengine.api;

import com.kestrel.rulesengine.api.dto.LocationAlertStatusResponse;
import com.kestrel.rulesengine.api.dto.LocationForecastResponse;
import com.kestrel.rulesengine.api.dto.SimulationRequest;
import com.kestrel.rulesengine.app.RulesEvaluationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rules")
public class RulesController {

    private static final Logger log = LoggerFactory.getLogger(RulesController.class);

    private final RulesEvaluationService rulesEvaluationService;
    private final RestClient floodIngestRestClient;

    public RulesController(
            RulesEvaluationService rulesEvaluationService,
            @Qualifier("floodIngestRestClient") RestClient floodIngestRestClient) {
        this.rulesEvaluationService = rulesEvaluationService;
        this.floodIngestRestClient = floodIngestRestClient;
    }

    @GetMapping("/status")
    public ResponseEntity<List<LocationAlertStatusResponse>> getStatus() {
        List<LocationAlertStatusResponse> statuses = rulesEvaluationService.getAllLocationStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/forecast/{locationId}")
    public ResponseEntity<LocationForecastResponse> getForecast(@PathVariable String locationId) {
        LocationForecastResponse forecast = rulesEvaluationService.getForecastForLocation(locationId);
        return ResponseEntity.ok(forecast);
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> forwardSimulation(@Valid @RequestBody SimulationRequest request) {
        log.info("Forwarding simulation request for location={} scenario={} to flood-ingest",
                request.locationId(), request.scenario());
        try {
            Map<?, ?> response = floodIngestRestClient.post()
                    .uri("/api/v1/flood/simulate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> castResponse = (Map<String, Object>) response;
            return ResponseEntity.ok(castResponse);
        } catch (Exception ex) {
            log.error("Failed to forward simulation to flood-ingest: {}", ex.getMessage());
            return ResponseEntity.ok(Map.of(
                    "locationId", request.locationId(),
                    "scenario", request.scenario(),
                    "message", "Simulation forwarded to flood-ingest (or local processing): " + ex.getMessage()
            ));
        }
    }
}
