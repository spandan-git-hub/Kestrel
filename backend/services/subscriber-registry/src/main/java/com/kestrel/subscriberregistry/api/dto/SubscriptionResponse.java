package com.kestrel.subscriberregistry.api.dto;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;

import java.time.Instant;
import java.util.Set;

public record SubscriptionResponse(
        String token,
        String locationId,
        Channel channel,
        String recipientIdentifier,
        String recipientHandle,
        Language language,
        Language preferredLanguage,
        Severity minSeverity,
        Set<Hazard> hazards,
        String status,
        boolean active,
        Instant createdAt
) {
    public static SubscriptionResponse fromEntity(SubscriptionEntity entity) {
        boolean isActive = "ACTIVE".equalsIgnoreCase(entity.getStatus());
        return new SubscriptionResponse(
                entity.getToken(),
                entity.getLocationId(),
                entity.getChannel(),
                entity.getRecipientIdentifier(),
                entity.getRecipientIdentifier(),
                entity.getLanguage(),
                entity.getLanguage(),
                entity.getMinSeverity(),
                entity.getHazardsAsEnums(),
                entity.getStatus(),
                isActive,
                entity.getCreatedAt()
        );
    }
}
