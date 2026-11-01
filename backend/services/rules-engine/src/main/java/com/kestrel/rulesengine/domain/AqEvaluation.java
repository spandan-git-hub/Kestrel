package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.Pollutant;
import com.kestrel.common.events.Severity;

import java.time.LocalDate;
import java.util.Map;

/**
 * Result of evaluating CPCB air quality criteria across current rolling averages and forecast horizon.
 */
public record AqEvaluation(
        Severity candidateSeverity,
        LocalDate firstExceedanceDate,
        int currentAqi,
        int peakAqi,
        Pollutant dominantPollutant,
        Map<Pollutant, Integer> currentSubIndices
) {}

