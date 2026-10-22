package com.kestrel.common.events;

public record SubscriptionChangedPayload(
    String token,
    String locationId,
    Channel channel,
    String recipientIdentifier,
    Language language,
    Severity minSeverity,
    String action
) {}
