package com.kestrel.alertfanout.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.app.SubscriberReplicaService;
import com.kestrel.alertfanout.infra.client.WebPushClient;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.WebPushDeliveryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Real W3C Web Push (VAPID) Worker consuming delivery tasks from SQS queue kestrel-delivery-webpush.
 * Dispatches browser push notifications using VAPID keys, marks expired endpoints as BLOCKED in DynamoDB,
 * and records delivery receipts in kestrel_fanout_delivery.
 */
@Component
public class WebPushDeliveryQueueConsumer extends SqsMessageHandler<WebPushDeliveryPayload> {

    private static final Logger log = LoggerFactory.getLogger(WebPushDeliveryQueueConsumer.class);

    private final WebPushClient webPushClient;
    private final SubscriberReplicaService subscriberReplicaService;
    private final DeliveryLogRepository deliveryLogRepository;

    @Autowired
    public WebPushDeliveryQueueConsumer(
            ObjectMapper objectMapper,
            WebPushClient webPushClient,
            SubscriberReplicaService subscriberReplicaService,
            DeliveryLogRepository deliveryLogRepository) {
        super(objectMapper, WebPushDeliveryPayload.class);
        this.webPushClient = webPushClient;
        this.subscriberReplicaService = subscriberReplicaService;
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Override
    public void processMessage(String messageBody) {
        try {
            JsonNode root = objectMapper.readTree(messageBody);

            // Unwrap SNS notification if present
            if (root.has("Type") && "Notification".equals(root.get("Type").asText()) && root.has("Message")) {
                String innerMessage = root.get("Message").asText();
                root = objectMapper.readTree(innerMessage);
            }

            WebPushDeliveryPayload payload;
            if (root.has("payload")) {
                payload = objectMapper.treeToValue(root.get("payload"), WebPushDeliveryPayload.class);
            } else {
                payload = objectMapper.treeToValue(root, WebPushDeliveryPayload.class);
            }

            log.info("Processing Web Push delivery SQS message: deliveryId={}, alertId={}, endpoint={}",
                    payload.deliveryId(), payload.alertId(), payload.endpoint());
            deliver(payload);
        } catch (Exception e) {
            log.error("SQS processing error on Web Push delivery queue; propagating exception for DLQ redrive: {}",
                    e.getMessage(), e);
            throw new RuntimeException("Web Push delivery processing error, triggering DLQ redrive", e);
        }
    }

    @Override
    public void handle(EventEnvelope<WebPushDeliveryPayload> envelope) {
        if (envelope != null && envelope.payload() != null) {
            deliver(envelope.payload());
        }
    }

    /**
     * Executes the Web Push notification dispatch, expired subscription handling, and receipt persistence.
     */
    public DeliveryLog deliver(WebPushDeliveryPayload payload) {
        if (payload == null) {
            log.warn("Ignoring null Web Push delivery payload");
            return null;
        }

        log.info("Dispatching Web Push notification for location={}, endpoint={}",
                payload.locationId(), payload.endpoint());

        // 1. Dispatch Web Push notification
        WebPushClient.WebPushDeliveryResult result = webPushClient.sendPush(payload);

        // 2. Handle HTTP 404/410 Expired Subscriptions -> mark subscriber BLOCKED in DynamoDB
        if ("EXPIRED".equalsIgnoreCase(result.status()) || result.statusCode() == 404 || result.statusCode() == 410) {
            log.warn("Browser push endpoint expired (HTTP {}) for subscriber token={}, locationId={}. Updating status to BLOCKED.",
                    result.statusCode(), payload.subscriberToken(), payload.locationId());
            if (payload.subscriberToken() != null && !payload.subscriberToken().isBlank() && payload.locationId() != null) {
                subscriberReplicaService.updateSubscriberStatus(payload.locationId(), payload.subscriberToken(), "BLOCKED");
            }
        }

        // 3. Record delivery receipt in DynamoDB table kestrel_fanout_delivery
        DeliveryLog deliveryLog = createDeliveryLog(payload, result.status());
        DeliveryLog saved = deliveryLogRepository.save(deliveryLog);
        log.info("Recorded Web Push delivery receipt: deliveryId={}, status={}", saved.getDeliveryId(), saved.getStatus());
        return saved;
    }

    private DeliveryLog createDeliveryLog(WebPushDeliveryPayload payload, String status) {
        String excerpt = (payload.title() != null ? payload.title() + ": " : "") + (payload.body() != null ? payload.body() : "");
        DeliveryLog deliveryLog = new DeliveryLog(
                payload.alertId(),
                payload.locationId(),
                payload.locationName(),
                payload.severity(),
                Channel.WEB_PUSH,
                payload.endpoint(),
                payload.language(),
                Instant.now(),
                truncate(excerpt, 1000),
                payload.provenance(),
                status
        );
        if (payload.deliveryId() != null && !payload.deliveryId().isBlank()) {
            deliveryLog.setDeliveryId(payload.deliveryId());
        }
        return deliveryLog;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}

