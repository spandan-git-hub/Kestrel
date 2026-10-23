package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.Severity;

public record StateTransition(
        Severity targetSeverity,
        Severity candidateSeverity,
        int candidateRuns,
        boolean severityChanged,
        boolean isStale,
        boolean shouldNotify,
        String reason
) {}
