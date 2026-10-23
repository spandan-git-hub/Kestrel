package com.kestrel.rulesengine.api;

import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.rulesengine.app.RulesEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rules/events")
public class RulesEventConsumerController {

    private static final Logger log = LoggerFactory.getLogger(RulesEventConsumerController.class);

    private final RulesEvaluationService rulesEvaluationService;

    public RulesEventConsumerController(RulesEvaluationService rulesEvaluationService) {
        this.rulesEvaluationService = rulesEvaluationService;
    }

    @PostMapping("/flood-signal")
    public ResponseEntity<Map<String, String>> onFloodSignalHttp(@RequestBody EventEnvelope<FloodSignalPayload> envelope) {
        log.info("Received FloodSignal event over HTTP: eventId={}, locationId={}",
                envelope.eventId(), envelope.payload().locationId());
        rulesEvaluationService.processFloodSignal(envelope.payload());
        return ResponseEntity.ok(Map.of(
                "status", "PROCESSED",
                "eventId", envelope.eventId()
        ));
    }

    @PostMapping("/location-upserted")
    public ResponseEntity<Map<String, String>> onLocationUpsertedHttp(@RequestBody EventEnvelope<LocationUpsertedPayload> envelope) {
        log.info("Received LocationUpserted event over HTTP: eventId={}, locationId={}",
                envelope.eventId(), envelope.payload().locationId());
        rulesEvaluationService.upsertLocationReplica(envelope.payload());
        return ResponseEntity.ok(Map.of(
                "status", "UPSERTED",
                "eventId", envelope.eventId()
        ));
    }

    @EventListener
    public void onFloodSignalLocal(EventEnvelope<?> envelope) {
        if ("FloodSignal".equals(envelope.eventType()) && envelope.payload() instanceof FloodSignalPayload payload) {
            log.info("Received FloodSignal event via local Spring ApplicationEvent: eventId={}, locationId={}",
                    envelope.eventId(), payload.locationId());
            rulesEvaluationService.processFloodSignal(payload);
        } else if ("LocationUpserted".equals(envelope.eventType()) && envelope.payload() instanceof LocationUpsertedPayload payload) {
            log.info("Received LocationUpserted event via local Spring ApplicationEvent: eventId={}, locationId={}",
                    envelope.eventId(), payload.locationId());
            rulesEvaluationService.upsertLocationReplica(payload);
        }
    }
}
