package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExceedanceCalculatorTest {

    private final ExceedanceCalculator calculator = new ExceedanceCalculator();

    @Test
    @DisplayName("Empty or null forecast returns NONE with null date and 0.0 peak")
    void emptyOrNullReturnsNone() {
        ExceedanceEvaluation nullResult = calculator.evaluate(null);
        assertThat(nullResult.candidateSeverity()).isEqualTo(Severity.NONE);
        assertThat(nullResult.firstExceedanceDate()).isNull();
        assertThat(nullResult.peakShare()).isEqualTo(0.0);

        ExceedanceEvaluation emptyResult = calculator.evaluate(Collections.emptyList());
        assertThat(emptyResult.candidateSeverity()).isEqualTo(Severity.NONE);
        assertThat(emptyResult.firstExceedanceDate()).isNull();
        assertThat(emptyResult.peakShare()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Calm forecast below 30% consensus returns NONE")
    void calmForecastReturnsNone() {
        LocalDate today = LocalDate.of(2026, 10, 23);
        List<ForecastDayStats> days = List.of(
                new ForecastDayStats(today, 10, 15, 20, 25, 30, 0.10, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(1), 12, 16, 22, 28, 32, 0.25, 0.05, 0.0),
                new ForecastDayStats(today.plusDays(2), 10, 14, 18, 24, 28, 0.15, 0.0, 0.0)
        );

        ExceedanceEvaluation result = calculator.evaluate(days);
        assertThat(result.candidateSeverity()).isEqualTo(Severity.NONE);
        assertThat(result.firstExceedanceDate()).isNull();
        assertThat(result.peakShare()).isEqualTo(0.0);
    }

    @ParameterizedTest(name = "Day {0} exceeds with share {1} -> candidate {2}")
    @CsvSource({
            "2, 0.42, WATCH",
            "1, 0.68, WARNING",
            "3, 0.78, SEVERE"
    })
    @DisplayName("Table-driven single-tier exceedance")
    void singleTierExceedance(int dayIndex, double share, Severity expectedSeverity) {
        LocalDate today = LocalDate.of(2026, 10, 23);
        List<ForecastDayStats> days = List.of(
                new ForecastDayStats(today, 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(1), 10, 15, 20, 25, 30,
                        expectedSeverity == Severity.WARNING ? share : 0.0,
                        expectedSeverity == Severity.WARNING ? share : 0.0,
                        0.0),
                new ForecastDayStats(today.plusDays(2), 10, 15, 20, 25, 30,
                        expectedSeverity == Severity.WATCH ? share : 0.0,
                        0.0,
                        0.0),
                new ForecastDayStats(today.plusDays(3), 10, 15, 20, 25, 30,
                        expectedSeverity == Severity.SEVERE ? 1.0 : 0.0,
                        expectedSeverity == Severity.SEVERE ? 0.9 : 0.0,
                        expectedSeverity == Severity.SEVERE ? share : 0.0)
        );

        ExceedanceEvaluation result = calculator.evaluate(days);
        assertThat(result.candidateSeverity()).isEqualTo(expectedSeverity);
        assertThat(result.firstExceedanceDate()).isEqualTo(today.plusDays(dayIndex));
        assertThat(result.peakShare()).isEqualTo(share);
    }

    @Test
    @DisplayName("Higher severity takes precedence over lower tier even if lower tier crests earlier")
    void precedenceSevereOverWarningAndWatch() {
        LocalDate today = LocalDate.of(2026, 10, 23);
        List<ForecastDayStats> days = List.of(
                // Day 0: 2yr reaches 0.50
                new ForecastDayStats(today, 20, 30, 40, 50, 60, 0.50, 0.10, 0.0),
                // Day 1: 5yr reaches 0.60
                new ForecastDayStats(today.plusDays(1), 30, 45, 60, 80, 100, 0.85, 0.60, 0.10),
                // Day 2: 20yr reaches 0.45
                new ForecastDayStats(today.plusDays(2), 50, 75, 100, 120, 150, 0.95, 0.80, 0.45)
        );

        ExceedanceEvaluation result = calculator.evaluate(days);
        assertThat(result.candidateSeverity()).isEqualTo(Severity.SEVERE);
        assertThat(result.firstExceedanceDate()).isEqualTo(today.plusDays(2));
        assertThat(result.peakShare()).isEqualTo(0.45);
    }

    @Test
    @DisplayName("Exceedance occurring outside 7-day horizon is ignored by default")
    void horizonCutoffIgnored() {
        LocalDate today = LocalDate.of(2026, 10, 23);
        List<ForecastDayStats> days = List.of(
                new ForecastDayStats(today, 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(1), 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(2), 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(3), 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(4), 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(5), 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                new ForecastDayStats(today.plusDays(6), 10, 15, 20, 25, 30, 0.0, 0.0, 0.0),
                // Day 7 (index 7, 8th day): SEVERE exceedance
                new ForecastDayStats(today.plusDays(7), 100, 150, 200, 250, 300, 1.0, 1.0, 0.80)
        );

        ExceedanceEvaluation result = calculator.evaluate(days, 7, 0.30);
        assertThat(result.candidateSeverity()).isEqualTo(Severity.NONE);
    }
}
