package com.kestrel.rulesengine.api;

import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.HeatSignalPayload;
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

    @PostMapping("/heat-signal")
    public ResponseEntity<Map<String, String>> onHeatSignalHttp(@RequestBody EventEnvelope<HeatSignalPayload> envelope) {
        log.info("Received HeatSignal event over HTTP: eventId={}, locationId={}",
                envelope.eventId(), envelope.payload().locationId());
        rulesEvaluationService.processHeatSignal(envelope.payload());
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

    @PostMapping("/provenance")
    public ResponseEntity<Map<String, String>> onProvenanceUpdatedHttp(
            @RequestBody EventEnvelope<com.kestrel.common.events.ProvenanceUpdatePayload> envelope) {
        log.info("Received ProvenanceUpdated event over HTTP: eventId={}, locationId={}, provenance={}",
                envelope.eventId(), envelope.payload().locationId(), envelope.payload().provenance());
        rulesEvaluationService.updateProvenance(envelope.payload().locationId(), envelope.payload().provenance());
        return ResponseEntity.ok(Map.of(
                "status", "UPDATED",
                "eventId", envelope.eventId(),
                "locationId", envelope.payload().locationId(),
                "provenance", envelope.payload().provenance().name()
        ));
    }

    @EventListener
    public void onFloodSignalLocal(EventEnvelope<?> envelope) {
        if ("FloodSignal".equals(envelope.eventType()) && envelope.payload() instanceof FloodSignalPayload payload) {
            log.info("Received FloodSignal event via local Spring ApplicationEvent: eventId={}, locationId={}",
                    envelope.eventId(), payload.locationId());
            rulesEvaluationService.processFloodSignal(payload);
        } else if ("HeatSignal".equals(envelope.eventType()) && envelope.payload() instanceof HeatSignalPayload payload) {
            log.info("Received HeatSignal event via local Spring ApplicationEvent: eventId={}, locationId={}",
                    envelope.eventId(), payload.locationId());
            rulesEvaluationService.processHeatSignal(payload);
        } else if ("LocationUpserted".equals(envelope.eventType()) && envelope.payload() instanceof LocationUpsertedPayload payload) {
            log.info("Received LocationUpserted event via local Spring ApplicationEvent: eventId={}, locationId={}",
                    envelope.eventId(), payload.locationId());
            rulesEvaluationService.upsertLocationReplica(payload);
        } else if ("ProvenanceUpdated".equals(envelope.eventType()) && envelope.payload() instanceof com.kestrel.common.events.ProvenanceUpdatePayload payload) {
            log.info("Received ProvenanceUpdated event via local Spring ApplicationEvent: eventId={}, locationId={}, provenance={}",
                    envelope.eventId(), payload.locationId(), payload.provenance());
            rulesEvaluationService.updateProvenance(payload.locationId(), payload.provenance());
        }
    }
}
