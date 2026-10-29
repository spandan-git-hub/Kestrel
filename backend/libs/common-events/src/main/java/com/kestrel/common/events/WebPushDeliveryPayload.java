package com.kestrel.common.events;

import java.time.Instant;

/**
 * Payload representing a localized W3C Web Push alert delivery job.
 * Dispatched to SQS queue kestrel-delivery-webpush and processed by WebPushDeliveryQueueConsumer.
 */
public record WebPushDeliveryPayload(
        String deliveryId,
        String alertId,
        String locationId,
        String locationName,
        Severity severity,
        String endpoint,
        String p256dhKey,
        String authKey,
        String subscriberToken,
        Language language,
        String title,
        String body,
        String clickUrl,
        Provenance provenance,
        Instant createdAt
) {
    public WebPushDeliveryPayload {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

