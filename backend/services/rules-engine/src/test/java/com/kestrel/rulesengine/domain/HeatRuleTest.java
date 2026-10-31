package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.HeatSignalPayload;
import com.kestrel.common.events.HeatSignalPayload.HeatDayStats;
import com.kestrel.common.events.RegionType;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeatRuleTest {

    private final HeatRule heatRule = new HeatRule();
    private final LocalDate today = LocalDate.of(2026, 5, 1);

    @Test
    @DisplayName("Regional temperature gates enforce PLAINS >= 40, COASTAL >= 37, HILLY >= 30")
    void regionalGatesAreEnforced() {
        assertThat(HeatRule.getRegionalGate(RegionType.PLAINS)).isEqualTo(40.0);
        assertThat(HeatRule.getRegionalGate(RegionType.COASTAL)).isEqualTo(37.0);
        assertThat(HeatRule.getRegionalGate(RegionType.HILLY)).isEqualTo(30.0);
        assertThat(HeatRule.getRegionalGate(null)).isEqualTo(40.0);
    }

    @Test
    @DisplayName("Temperature below regional gate suppresses heat-wave classification despite high departure")
    void belowGateSuppressesHeatWave() {
        // Plains gate is 40.0°C; Tmax is 39.5°C with departure +6.8°C
        boolean isHw = HeatRule.isHeatWaveDay(39.5, 6.8, RegionType.PLAINS);
        boolean isSevere = HeatRule.isSevereHeatWaveDay(39.5, 6.8, RegionType.PLAINS);

        assertThat(isHw).isFalse();
        assertThat(isSevere).isFalse();

        // Coastal gate is 37.0°C; 39.5°C meets gate and departure +6.8°C triggers severe
        assertThat(HeatRule.isHeatWaveDay(39.5, 6.8, RegionType.COASTAL)).isTrue();
        assertThat(HeatRule.isSevereHeatWaveDay(39.5, 6.8, RegionType.COASTAL)).isTrue();

        // Hilly gate is 30.0°C; 31.0°C meets gate and departure +5.0°C triggers heat wave
        assertThat(HeatRule.isHeatWaveDay(31.0, 5.0, RegionType.HILLY)).isTrue();
        assertThat(HeatRule.isSevereHeatWaveDay(31.0, 5.0, RegionType.HILLY)).isFalse();
    }

    @Test
    @DisplayName("Absolute temperature thresholds trigger heat-wave (>=45°C) and severe (>=47°C)")
    void absoluteTemperatureThresholdsTriggerHeatWave() {
        // Departure is only +2.0°C, but Tmax is 45.5°C (in Plains)
        assertThat(HeatRule.isHeatWaveDay(45.5, 2.0, RegionType.PLAINS)).isTrue();
        assertThat(HeatRule.isSevereHeatWaveDay(45.5, 2.0, RegionType.PLAINS)).isFalse();

        // Tmax is 47.2°C (in Plains) -> Severe Heat-Wave
        assertThat(HeatRule.isHeatWaveDay(47.2, 2.0, RegionType.PLAINS)).isTrue();
        assertThat(HeatRule.isSevereHeatWaveDay(47.2, 2.0, RegionType.PLAINS)).isTrue();
    }

    @Test
    @DisplayName("Evaluation with 0 heat-wave days returns Severity.NONE")
    void calmForecastReturnsNone() {
        List<HeatDayStats> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(new HeatDayStats(today.plusDays(i), 38.0, 41.0, 40.0, 37.0, 1.0));
        }

        HeatEvaluation eval = heatRule.evaluate(RegionType.PLAINS, days);
        assertThat(eval.candidateSeverity()).isEqualTo(Severity.NONE);
        assertThat(eval.firstExceedanceDate()).isNull();
        assertThat(eval.heatWaveDaysCount()).isEqualTo(0);
        assertThat(eval.severeHeatWaveDaysCount()).isEqualTo(0);
        assertThat(eval.peakDeparture()).isEqualTo(1.0);
        assertThat(eval.peakTmax()).isEqualTo(38.0);
    }

    @Test
    @DisplayName("Evaluation with 1 isolated heat-wave day returns Severity.WATCH")
    void singleHeatWaveDayReturnsWatch() {
        List<HeatDayStats> days = new ArrayList<>();
        days.add(new HeatDayStats(today, 38.0, 41.0, 40.0, 37.0, 1.0));
        // Day 1 meets gate 40°C and departure >= 4.5°C (42.5°C, departure +4.8°C)
        days.add(new HeatDayStats(today.plusDays(1), 42.5, 46.0, 35.0, 37.7, 4.8));
        days.add(new HeatDayStats(today.plusDays(2), 38.0, 41.0, 40.0, 37.0, 1.0));

        HeatEvaluation eval = heatRule.evaluate(RegionType.PLAINS, days);
        assertThat(eval.candidateSeverity()).isEqualTo(Severity.WATCH);
        assertThat(eval.firstExceedanceDate()).isEqualTo(today.plusDays(1));
        assertThat(eval.heatWaveDaysCount()).isEqualTo(1);
        assertThat(eval.severeHeatWaveDaysCount()).isEqualTo(0);
        assertThat(eval.peakDeparture()).isEqualTo(4.8);
    }

    @Test
    @DisplayName("Evaluation with 2 non-consecutive heat-wave days returns Severity.WATCH")
    void nonConsecutiveHeatWaveDaysReturnsWatch() {
        List<HeatDayStats> days = new ArrayList<>();
        // Day 0: HW
        days.add(new HeatDayStats(today, 42.0, 45.0, 35.0, 37.0, 5.0));
        // Day 1: Calm
        days.add(new HeatDayStats(today.plusDays(1), 38.0, 40.0, 40.0, 37.0, 1.0));
        // Day 2: HW
        days.add(new HeatDayStats(today.plusDays(2), 43.0, 46.0, 35.0, 37.0, 6.0));

        HeatEvaluation eval = heatRule.evaluate(RegionType.PLAINS, days);
        assertThat(eval.candidateSeverity()).isEqualTo(Severity.WATCH);
        assertThat(eval.firstExceedanceDate()).isEqualTo(today);
        assertThat(eval.heatWaveDaysCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Evaluation with 2 consecutive heat-wave days returns Severity.WARNING")
    void consecutiveHeatWaveDaysReturnsWarning() {
        List<HeatDayStats> days = new ArrayList<>();
        days.add(new HeatDayStats(today, 38.0, 40.0, 40.0, 37.0, 1.0));
        // Day 1 & Day 2: consecutive HW days
        days.add(new HeatDayStats(today.plusDays(1), 42.0, 45.0, 35.0, 37.0, 5.0));
        days.add(new HeatDayStats(today.plusDays(2), 43.0, 46.0, 35.0, 37.0, 6.0));
        days.add(new HeatDayStats(today.plusDays(3), 38.0, 40.0, 40.0, 37.0, 1.0));

        HeatEvaluation eval = heatRule.evaluate(RegionType.PLAINS, days);
        assertThat(eval.candidateSeverity()).isEqualTo(Severity.WARNING);
        assertThat(eval.firstExceedanceDate()).isEqualTo(today.plusDays(1));
        assertThat(eval.heatWaveDaysCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Evaluation with 2 consecutive severe heat-wave days returns Severity.SEVERE")
    void consecutiveSevereHeatWaveDaysReturnsSevere() {
        List<HeatDayStats> days = new ArrayList<>();
        // Day 0 & Day 1: consecutive severe HW days (departure > 6.4°C, Tmax >= 44°C)
        days.add(new HeatDayStats(today, 44.5, 48.0, 30.0, 37.5, 7.0));
        days.add(new HeatDayStats(today.plusDays(1), 45.0, 49.0, 30.0, 37.5, 7.5));
        days.add(new HeatDayStats(today.plusDays(2), 40.0, 43.0, 35.0, 37.5, 2.5));

        HeatEvaluation eval = heatRule.evaluate(RegionType.PLAINS, days);
        assertThat(eval.candidateSeverity()).isEqualTo(Severity.SEVERE);
        assertThat(eval.firstExceedanceDate()).isEqualTo(today);
        assertThat(eval.severeHeatWaveDaysCount()).isEqualTo(2);
        assertThat(eval.peakDeparture()).isEqualTo(7.5);
    }

    @Test
    @DisplayName("Evaluation with 1 HW day and 1 severe day consecutive returns Severity.WARNING")
    void consecutiveHwAndSevereReturnsWarning() {
        List<HeatDayStats> days = new ArrayList<>();
        // Day 0: Regular HW (departure 5.0)
        days.add(new HeatDayStats(today, 42.0, 45.0, 35.0, 37.0, 5.0));
        // Day 1: Severe HW (departure 7.0)
        days.add(new HeatDayStats(today.plusDays(1), 44.5, 48.0, 30.0, 37.0, 7.5));

        HeatEvaluation eval = heatRule.evaluate(RegionType.PLAINS, days);
        // Only 1 severe day, but 2 consecutive HW days
        assertThat(eval.candidateSeverity()).isEqualTo(Severity.WARNING);
        assertThat(eval.firstExceedanceDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("Evaluation using HeatSignalPayload object correctly unrolls regionType and days")
    void evaluateUsingPayload() {
        List<HeatDayStats> days = List.of(
                new HeatDayStats(today, 43.0, 46.0, 30.0, 37.0, 6.0)
        );
        HeatSignalPayload payload = new HeatSignalPayload(
                "yamuna-delhi",
                "run-123",
                Instant.now(),
                Instant.now(),
                RegionType.PLAINS,
                days
        );

        HeatEvaluation eval = heatRule.evaluate(payload);
        assertThat(eval.candidateSeverity()).isEqualTo(Severity.WATCH);
        assertThat(eval.firstExceedanceDate()).isEqualTo(today);
    }
}

