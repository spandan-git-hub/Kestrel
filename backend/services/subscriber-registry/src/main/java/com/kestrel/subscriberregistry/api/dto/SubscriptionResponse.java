package com.kestrel.subscriberregistry.api.dto;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;

import java.time.Instant;

public record SubscriptionResponse(
        String token,
        String locationId,
        Channel channel,
        String recipientIdentifier,
        Language language,
        Severity minSeverity,
        String status,
        Instant createdAt
) {
    public static SubscriptionResponse fromEntity(SubscriptionEntity entity) {
        return new SubscriptionResponse(
                entity.getToken(),
                entity.getLocationId(),
                entity.getChannel(),
                entity.getRecipientIdentifier(),
                entity.getLanguage(),
                entity.getMinSeverity(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
