package com.kestrel.common.events;

import java.time.Instant;

/**
 * Payload representing a localized Telegram alert delivery job.
 * Dispatched to SQS queue kestrel-delivery-telegram and processed by TelegramDeliveryQueueConsumer.
 */
public record TelegramDeliveryPayload(
        String deliveryId,
        String alertId,
        String locationId,
        String locationName,
        Severity severity,
        String chatId,
        String subscriberToken,
        Language language,
        String messageText,
        Provenance provenance,
        String whatsAppShareUrl,
        Instant createdAt
) {
    public TelegramDeliveryPayload {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

