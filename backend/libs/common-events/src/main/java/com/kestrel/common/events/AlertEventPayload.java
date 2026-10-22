package com.kestrel.common.events;

import java.time.Instant;
import java.time.LocalDate;

public record AlertEventPayload(
    String alertId,
    String locationId,
    Hazard hazard,
    String stage,             // e.g. "DECISION"
    Severity fromSeverity,
    Severity toSeverity,
    Provenance provenance,
    Instant timestamp,
    LocalDate firstExceedanceDate,
    double peakShare,
    boolean autoSend
) {}
