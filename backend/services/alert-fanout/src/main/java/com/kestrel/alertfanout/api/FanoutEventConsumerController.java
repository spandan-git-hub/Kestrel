package com.kestrel.alertfanout.api;

import com.kestrel.alertfanout.app.LocationReplicaService;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.LocationUpsertedPayload;
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
@RequestMapping("/api/v1/fanout/events")
public class FanoutEventConsumerController {

    private static final Logger log = LoggerFactory.getLogger(FanoutEventConsumerController.class);

    private final LocationReplicaService locationReplicaService;
    private final com.kestrel.alertfanout.app.AlertProcessingService alertProcessingService;

    public FanoutEventConsumerController(
            LocationReplicaService locationReplicaService,
            com.kestrel.alertfanout.app.AlertProcessingService alertProcessingService) {
        this.locationReplicaService = locationReplicaService;
        this.alertProcessingService = alertProcessingService;
    }

    @PostMapping("/location-upserted")
    public ResponseEntity<Map<String, String>> onLocationUpsertedHttp(
            @RequestBody EventEnvelope<LocationUpsertedPayload> envelope) {
        log.info("Received LocationUpserted event over HTTP: eventId={}, locationId={}",
                envelope.eventId(), envelope.payload().locationId());
        LocationReplica saved = locationReplicaService.upsertLocationReplica(envelope.payload());
        return ResponseEntity.ok(Map.of(
                "status", "REPLICATED",
                "locationId", saved.getLocationId(),
                "eventId", envelope.eventId()
        ));
    }

    @PostMapping("/alert")
    public ResponseEntity<Map<String, Object>> onAlertEventHttp(
            @RequestBody EventEnvelope<com.kestrel.common.events.AlertEventPayload> envelope) {
        log.info("Received AlertEvent over HTTP: eventId={}, locationId={}, stage={}",
                envelope.eventId(), envelope.payload().locationId(), envelope.payload().stage());
        com.kestrel.alertfanout.infra.entity.ActiveAlertEntity activeAlert =
                alertProcessingService.processAlertEvent(envelope.payload());
        return ResponseEntity.ok(Map.of(
                "status", "PROCESSED",
                "alertId", activeAlert.getAlertId(),
                "locationId", activeAlert.getLocationId(),
                "severity", activeAlert.getToSeverity().name(),
                "eventId", envelope.eventId()
        ));
    }

    @EventListener
    public void onLocalEventEnvelope(EventEnvelope<?> envelope) {
        if ("LocationUpserted".equals(envelope.eventType()) && envelope.payload() instanceof LocationUpsertedPayload payload) {
            log.info("Received LocationUpserted event via local Spring ApplicationEvent: eventId={}, locationId={}",
                    envelope.eventId(), payload.locationId());
            locationReplicaService.upsertLocationReplica(payload);
        } else if ("AlertEvent".equals(envelope.eventType()) && envelope.payload() instanceof com.kestrel.common.events.AlertEventPayload payload) {
            log.info("Received AlertEvent via local Spring ApplicationEvent: eventId={}, locationId={}, stage={}",
                    envelope.eventId(), payload.locationId(), payload.stage());
            alertProcessingService.processAlertEvent(payload);
        }
    }
}
