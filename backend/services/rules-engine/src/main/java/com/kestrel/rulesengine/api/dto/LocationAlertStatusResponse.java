package com.kestrel.rulesengine.api.dto;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;

import java.time.Instant;
import java.time.LocalDate;

public record LocationAlertStatusResponse(
        String locationId,
        String locationName,
        Hazard hazard,
        Severity severity,
        Provenance provenance,
        String activeAlertId,
        LocalDate firstExceedanceDate,
        Double peakExceedanceShare,
        Instant dataAsOf,
        Long dataAgeSeconds,
        boolean isStale,
        Instant lastTransitionAt
) {}
