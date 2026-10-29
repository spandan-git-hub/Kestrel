package com.kestrel.rulesengine.api;

import com.kestrel.rulesengine.api.dto.LocationAlertStatusResponse;
import com.kestrel.rulesengine.api.dto.LocationForecastResponse;
import com.kestrel.rulesengine.app.RulesEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping({"/sync-live", "/sync-live-all"})
    public ResponseEntity<Map<?, ?>> triggerLiveSync() {
        log.info("Triggering real Open-Meteo flood forecast synchronization via flood-ingest");
        try {
            Map<?, ?> response = floodIngestRestClient.post()
                    .uri("/api/v1/flood/sync-live")
                    .retrieve()
                    .body(Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to forward live sync to flood-ingest: {}", ex.getMessage());
            return ResponseEntity.status(502).body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to trigger live sync: " + ex.getMessage()
            ));
        }
    }
}
