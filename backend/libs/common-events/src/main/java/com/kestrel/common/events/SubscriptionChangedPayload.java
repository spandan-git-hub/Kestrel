package com.kestrel.common.events;

import java.util.Set;

public record SubscriptionChangedPayload(
    String token,
    String locationId,
    Channel channel,
    String recipientIdentifier,
    Language language,
    Severity minSeverity,
    Set<Hazard> hazards,
    String action
) {
    public SubscriptionChangedPayload(
        String token,
        String locationId,
        Channel channel,
        String recipientIdentifier,
        Language language,
        Severity minSeverity,
        String action
    ) {
        this(token, locationId, channel, recipientIdentifier, language, minSeverity, Set.of(Hazard.FLOOD, Hazard.HEAT, Hazard.AIR_QUALITY), action);
    }
}
