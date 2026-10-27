package com.kestrel.common.core.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base consumer handler for SQS messages wrapping EventEnvelope<T>.
 * Automatically handles direct JSON envelopes or SNS envelope wrappers.
 * Re-throws exceptions on processing failure to trigger SQS DLQ redrive policies.
 */
public abstract class SqsMessageHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageHandler.class);

    protected final ObjectMapper objectMapper;
    protected final Class<T> payloadClass;

    protected SqsMessageHandler(ObjectMapper objectMapper, Class<T> payloadClass) {
        this.objectMapper = objectMapper;
        this.payloadClass = payloadClass;
    }

    public abstract void handle(EventEnvelope<T> envelope);

    public void processMessage(String messageBody) {
        try {
            JsonNode root = objectMapper.readTree(messageBody);
            String payloadJson = messageBody;

            // Detect if wrapped by SNS notification JSON
            if (root.has("Type") && "Notification".equals(root.get("Type").asText()) && root.has("Message")) {
                payloadJson = root.get("Message").asText();
                root = objectMapper.readTree(payloadJson);
            }

            String eventId = root.has("eventId") ? root.get("eventId").asText() : "unknown";
            String eventType = root.has("eventType") ? root.get("eventType").asText() : "unknown";
            String occurredAt = root.has("occurredAt") ? root.get("occurredAt").asText() : null;
            String producer = root.has("producer") ? root.get("producer").asText() : null;

            T payload = null;
            if (root.has("payload")) {
                payload = objectMapper.treeToValue(root.get("payload"), payloadClass);
            }

            int schemaVersion = root.has("schemaVersion") ? root.get("schemaVersion").asInt() : 1;
            String correlationId = root.has("correlationId") ? root.get("correlationId").asText() : null;

            EventEnvelope<T> envelope = new EventEnvelope<>(
                    eventId,
                    eventType,
                    schemaVersion,
                    occurredAt != null ? java.time.Instant.parse(occurredAt) : java.time.Instant.now(),
                    producer,
                    correlationId,
                    payload
            );

            log.info("Processing SQS event {} of type {}", eventId, eventType);
            handle(envelope);
        } catch (Exception e) {
            log.error("SQS message processing failed; propagating exception for DLQ redrive: {}", e.getMessage(), e);
            throw new RuntimeException("SQS processing error, triggering redrive", e);
        }
    }
}
