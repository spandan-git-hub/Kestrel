package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.Severity;

import java.time.LocalDate;

/**
 * Result of evaluating IMD heat-wave criteria across a multi-day forecast window.
 */
public record HeatEvaluation(
        Severity candidateSeverity,
        LocalDate firstExceedanceDate,
        double peakDeparture,
        double peakTmax,
        int heatWaveDaysCount,
        int severeHeatWaveDaysCount
) {}

