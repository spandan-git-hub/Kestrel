package com.kestrel.floodingest.api.dto;

public record SyncLiveResponse(
    String message,
    int syncedLocations
) {}
