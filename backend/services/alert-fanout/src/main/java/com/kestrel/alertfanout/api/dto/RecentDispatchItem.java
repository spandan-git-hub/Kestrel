package com.kestrel.alertfanout.api.dto;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;

import java.time.Instant;

/**
 * Real alert delivery record DTO queried from DynamoDB table kestrel_fanout_delivery.
 */
public record RecentDispatchItem(
        Long id,
        String alertId,
        String locationId,
        String locationName,
        Severity severity,
        Channel channel,
        String recipient,
        Language language,
        Instant timestamp,
        String messageExcerpt,
        Provenance provenance,
        String status,
        String whatsAppUrl
) {}

