package com.kestrel.floodingest.domain;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExceedanceCalculatorTest {

    private final LocalDate testDate = LocalDate.of(2026, 10, 22);

    @Test
    @DisplayName("Calculates quantiles and exceedance shares for 50 ensemble members")
    void shouldCalculateFor50Members() {
        // Create 50 members: 10 below T2 (45), 20 between T2 and T5 (85), 15 between T5 and T20 (140), 5 above T20
        List<Double> members = new ArrayList<>();
        for (int i = 0; i < 10; i++) members.add(30.0);
        for (int i = 0; i < 20; i++) members.add(60.0);
        for (int i = 0; i < 15; i++) members.add(100.0);
        for (int i = 0; i < 5; i++) members.add(160.0);

        ForecastDayStats stats = ExceedanceCalculator.calculateDayStats(
                testDate, members, 45.0, 85.0, 140.0);

        assertThat(stats.date()).isEqualTo(testDate);
        assertThat(stats.qMin()).isEqualTo(30.0);
        assertThat(stats.qMax()).isEqualTo(160.0);

        // Members >= 45: 20 + 15 + 5 = 40 / 50 = 0.80
        assertThat(stats.share2yr()).isEqualTo(0.80);
        // Members >= 85: 15 + 5 = 20 / 50 = 0.40
        assertThat(stats.share5yr()).isEqualTo(0.40);
        // Members >= 140: 5 / 50 = 0.10
        assertThat(stats.share20yr()).isEqualTo(0.10);
    }

    @Test
    @DisplayName("Calculates accurately for 51 ensemble members (GloFAS odd count)")
    void shouldCalculateFor51Members() {
        List<Double> members = new ArrayList<>();
        // 51 members
        for (int i = 0; i < 17; i++) members.add(40.0); // < 50
        for (int i = 0; i < 17; i++) members.add(75.0); // >= 50, < 100
        for (int i = 0; i < 17; i++) members.add(120.0); // >= 100

        ForecastDayStats stats = ExceedanceCalculator.calculateDayStats(
                testDate, members, 50.0, 100.0, 150.0);

        assertThat(stats.qMin()).isEqualTo(40.0);
        assertThat(stats.qMax()).isEqualTo(120.0);

        // >= 50: 34 / 51 = 0.6666... rounded to 0.67
        assertThat(stats.share2yr()).isEqualTo(0.67);
        // >= 100: 17 / 51 = 0.3333... rounded to 0.33
        assertThat(stats.share5yr()).isEqualTo(0.33);
        // >= 150: 0 / 51 = 0.00
        assertThat(stats.share20yr()).isEqualTo(0.00);
    }

    @Test
    @DisplayName("calculateForecast processes full 10-day matrix across multiple members")
    void shouldCalculateMultiDayForecast() {
        List<LocalDate> dates = List.of(testDate, testDate.plusDays(1));
        Map<String, List<Double>> memberDischarges = new HashMap<>();

        memberDischarges.put("member01", List.of(20.0, 100.0));
        memberDischarges.put("member02", List.of(25.0, 110.0));
        memberDischarges.put("member03", List.of(30.0, 120.0));
        memberDischarges.put("member04", List.of(35.0, 40.0));

        List<ForecastDayStats> result = ExceedanceCalculator.calculateForecast(
                dates, memberDischarges, 50.0, 90.0, 150.0);

        assertThat(result).hasSize(2);

        // Day 1: all values 20, 25, 30, 35 < 50
        assertThat(result.get(0).share2yr()).isEqualTo(0.0);

        // Day 2: 100, 110, 120, 40. 3 of 4 are >= 50 (0.75) and >= 90 (0.75)
        assertThat(result.get(1).share2yr()).isEqualTo(0.75);
        assertThat(result.get(1).share5yr()).isEqualTo(0.75);
        assertThat(result.get(1).share20yr()).isEqualTo(0.0);
    }
}
