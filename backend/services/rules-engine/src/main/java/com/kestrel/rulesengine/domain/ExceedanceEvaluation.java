package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.Severity;

import java.time.LocalDate;

public record ExceedanceEvaluation(
        Severity candidateSeverity,
        LocalDate firstExceedanceDate,
        double peakShare
) {}
