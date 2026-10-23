package com.kestrel.subscriberregistry.api.dto;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionRequest(
        @NotBlank(message = "locationId is required")
        String locationId,

        @NotNull(message = "channel is required")
        Channel channel,

        @NotBlank(message = "recipientIdentifier is required")
        String recipientIdentifier,

        @NotNull(message = "language is required")
        Language language,

        @NotNull(message = "minSeverity is required")
        Severity minSeverity
) {}
