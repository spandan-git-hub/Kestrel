package com.kestrel.alertfanout.api;

import com.kestrel.alertfanout.app.AlertProcessingService;
import com.kestrel.alertfanout.app.LocationReplicaService;
import com.kestrel.alertfanout.app.SubscriberReplicaService;
import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.SubscriptionChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fanout/events")
public class FanoutEventConsumerController {

    private static final Logger log = LoggerFactory.getLogger(FanoutEventConsumerController.class);

    private final LocationReplicaService locationReplicaService;
    private final SubscriberReplicaService subscriberReplicaService;
    private final AlertProcessingService alertProcessingService;
    private final com.kestrel.alertfanout.consumer.TelegramDeliveryQueueConsumer telegramDeliveryConsumer;
    private final com.kestrel.alertfanout.consumer.WebPushDeliveryQueueConsumer webPushDeliveryConsumer;

    public FanoutEventConsumerController(
            LocationReplicaService locationReplicaService,
            SubscriberReplicaService subscriberReplicaService,
            AlertProcessingService alertProcessingService,
            com.kestrel.alertfanout.consumer.TelegramDeliveryQueueConsumer telegramDeliveryConsumer,
            com.kestrel.alertfanout.consumer.WebPushDeliveryQueueConsumer webPushDeliveryConsumer) {
        this.locationReplicaService = locationReplicaService;
        this.subscriberReplicaService = subscriberReplicaService;
        this.alertProcessingService = alertProcessingService;
        this.telegramDeliveryConsumer = telegramDeliveryConsumer;
        this.webPushDeliveryConsumer = webPushDeliveryConsumer;
    }

    @PostMapping("/telegram-delivery")
    public ResponseEntity<com.kestrel.alertfanout.infra.entity.DeliveryLog> onTelegramDeliveryHttp(
            @RequestBody com.kestrel.common.events.TelegramDeliveryPayload payload) {
        log.info("Received TelegramDelivery over HTTP: deliveryId={}, alertId={}, chatId={}",
                payload.deliveryId(), payload.alertId(), payload.chatId());
        com.kestrel.alertfanout.infra.entity.DeliveryLog log = telegramDeliveryConsumer.deliver(payload);
        return ResponseEntity.ok(log);
    }

    @PostMapping("/webpush-delivery")
    public ResponseEntity<com.kestrel.alertfanout.infra.entity.DeliveryLog> onWebPushDeliveryHttp(
            @RequestBody com.kestrel.common.events.WebPushDeliveryPayload payload) {
        log.info("Received WebPushDelivery over HTTP: deliveryId={}, alertId={}, endpoint={}",
                payload.deliveryId(), payload.alertId(), payload.endpoint());
        com.kestrel.alertfanout.infra.entity.DeliveryLog log = webPushDeliveryConsumer.deliver(payload);
        return ResponseEntity.ok(log);
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

    @PostMapping("/subscription-changed")
    public ResponseEntity<Map<String, String>> onSubscriptionChangedHttp(
            @RequestBody EventEnvelope<SubscriptionChangedPayload> envelope) {
        log.info("Received SubscriptionChanged event over HTTP: eventId={}, token={}, locationId={}, action={}",
                envelope.eventId(), envelope.payload().token(), envelope.payload().locationId(), envelope.payload().action());
        subscriberReplicaService.replicateSubscription(envelope.payload());
        String status = "DELETED".equalsIgnoreCase(envelope.payload().action()) ? "DELETED" : "REPLICATED";
        return ResponseEntity.ok(Map.of(
                "status", status,
                "token", envelope.payload().token(),
                "eventId", envelope.eventId()
        ));
    }

    @GetMapping("/subscribers/{locationId}")
    public ResponseEntity<List<SubscriberReplica>> getSubscribersByLocation(@PathVariable String locationId) {
        return ResponseEntity.ok(subscriberReplicaService.getSubscribersByLocation(locationId));
    }

    @PostMapping("/alert")
    public ResponseEntity<Map<String, Object>> onAlertEventHttp(
            @RequestBody EventEnvelope<AlertEventPayload> envelope) {
        log.info("Received AlertEvent over HTTP: eventId={}, locationId={}, stage={}",
                envelope.eventId(), envelope.payload().locationId(), envelope.payload().stage());
        ActiveAlertEntity activeAlert = alertProcessingService.processAlertEvent(envelope.payload());
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
        } else if ("SubscriptionChanged".equals(envelope.eventType()) && envelope.payload() instanceof SubscriptionChangedPayload payload) {
            log.info("Received SubscriptionChanged event via local Spring ApplicationEvent: eventId={}, token={}, action={}",
                    envelope.eventId(), payload.token(), payload.action());
            subscriberReplicaService.replicateSubscription(payload);
        } else if ("AlertEvent".equals(envelope.eventType()) && envelope.payload() instanceof AlertEventPayload payload) {
            log.info("Received AlertEvent via local Spring ApplicationEvent: eventId={}, locationId={}, stage={}",
                    envelope.eventId(), payload.locationId(), payload.stage());
            alertProcessingService.processAlertEvent(payload);
        }
    }
}
