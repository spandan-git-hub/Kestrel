package com.kestrel.alertfanout.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.app.LocationReplicaService;
import com.kestrel.alertfanout.app.SubscriberReplicaService;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.SubscriptionChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FanoutRegistryQueueConsumer extends SqsMessageHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(FanoutRegistryQueueConsumer.class);

    private final LocationReplicaService locationReplicaService;
    private final SubscriberReplicaService subscriberReplicaService;

    public FanoutRegistryQueueConsumer(
            ObjectMapper objectMapper,
            LocationReplicaService locationReplicaService,
            SubscriberReplicaService subscriberReplicaService) {
        super(objectMapper, Object.class);
        this.locationReplicaService = locationReplicaService;
        this.subscriberReplicaService = subscriberReplicaService;
    }

    @Override
    public void processMessage(String messageBody) {
        try {
            JsonNode root = objectMapper.readTree(messageBody);

            // Detect if wrapped by SNS notification JSON
            if (root.has("Type") && "Notification".equals(root.get("Type").asText()) && root.has("Message")) {
                String innerMessage = root.get("Message").asText();
                root = objectMapper.readTree(innerMessage);
            }

            String eventId = root.has("eventId") ? root.get("eventId").asText() : "unknown";
            String eventType = root.has("eventType") ? root.get("eventType").asText() : "unknown";

            log.info("Processing FanoutRegistry SQS event {} of type {}", eventId, eventType);

            if ("LocationUpserted".equalsIgnoreCase(eventType)) {
                if (root.has("payload")) {
                    LocationUpsertedPayload payload = objectMapper.treeToValue(root.get("payload"), LocationUpsertedPayload.class);
                    locationReplicaService.upsertLocationReplica(payload);
                }
            } else if ("SubscriptionChanged".equalsIgnoreCase(eventType)) {
                if (root.has("payload")) {
                    SubscriptionChangedPayload payload = objectMapper.treeToValue(root.get("payload"), SubscriptionChangedPayload.class);
                    subscriberReplicaService.replicateSubscription(payload);
                }
            } else {
                log.warn("Unrecognized event type on kestrel-fanout-registry queue: {}", eventType);
            }
        } catch (Exception e) {
            log.error("SQS message processing failed on kestrel-fanout-registry; propagating exception for DLQ redrive: {}", e.getMessage(), e);
            throw new RuntimeException("SQS processing error, triggering redrive", e);
        }
    }

    @Override
    public void handle(EventEnvelope<Object> envelope) {
        if (envelope == null || envelope.payload() == null) return;
        if (envelope.payload() instanceof LocationUpsertedPayload loc) {
            locationReplicaService.upsertLocationReplica(loc);
        } else if (envelope.payload() instanceof SubscriptionChangedPayload sub) {
            subscriberReplicaService.replicateSubscription(sub);
        }
    }
}
