package com.kestrel.domain.events;

import com.kestrel.domain.Hazard;
import com.kestrel.domain.Severity;

public record BroadcastRequestedEvent(
    String locationId,
    Hazard hazard,
    Severity severity,
    String customNote,
    String coordinatorId
) {}
