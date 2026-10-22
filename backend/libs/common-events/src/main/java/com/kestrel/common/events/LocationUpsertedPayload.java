package com.kestrel.common.events;

public record LocationUpsertedPayload(
    String locationId,
    String name,
    String state,
    double latitude,
    double longitude,
    String riverName,
    double threshold2yr,
    double threshold5yr,
    double threshold20yr,
    Language primaryLanguage,
    boolean active
) {}
