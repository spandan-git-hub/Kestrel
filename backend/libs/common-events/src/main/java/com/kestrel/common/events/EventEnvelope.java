package com.kestrel.common.events;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
    String eventId,
    String eventType,
    int schemaVersion,
    Instant occurredAt,
    String producer,
    String correlationId,
    T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, String producer, String correlationId, T payload) {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            eventType,
            1,
            Instant.now(),
            producer,
            correlationId,
            payload
        );
    }
}
