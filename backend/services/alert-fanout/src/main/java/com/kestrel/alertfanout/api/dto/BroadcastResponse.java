package com.kestrel.alertfanout.api.dto;

import com.kestrel.common.events.Severity;

import java.time.Instant;

public record BroadcastResponse(
        String broadcastId,
        String locationId,
        String locationName,
        Severity severity,
        int dispatchedCount,
        String status,
        String whatsAppShareUrl,
        Instant timestamp,
        String message
) {}
