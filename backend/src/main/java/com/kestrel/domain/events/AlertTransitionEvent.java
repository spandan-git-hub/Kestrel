package com.kestrel.domain.events;

import com.kestrel.domain.Hazard;
import com.kestrel.domain.Provenance;
import com.kestrel.domain.Severity;
import java.time.Instant;

public record AlertTransitionEvent(
    String alertId,
    String locationId,
    Hazard hazard,
    Severity fromSeverity,
    Severity toSeverity,
    Provenance provenance,
    Instant timestamp,
    String firstExceedanceDate
) {}
