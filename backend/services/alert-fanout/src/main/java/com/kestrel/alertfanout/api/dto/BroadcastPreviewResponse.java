package com.kestrel.alertfanout.api.dto;

import com.kestrel.common.events.Severity;

import java.util.Map;

public record BroadcastPreviewResponse(
        String locationId,
        String locationName,
        Severity severity,
        int recipientCount,
        Map<String, String> previews,
        String whatsAppShareUrl
) {}
