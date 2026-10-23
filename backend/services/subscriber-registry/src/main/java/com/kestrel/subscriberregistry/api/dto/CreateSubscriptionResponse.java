package com.kestrel.subscriberregistry.api.dto;

public record CreateSubscriptionResponse(
        String token,
        String status,
        String message
) {}
