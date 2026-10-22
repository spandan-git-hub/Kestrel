package com.kestrel.floodingest.domain;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.SimulationScenario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScenarioSimulator {

    public static List<ForecastDayStats> generateScenario(
            SimulationScenario scenario,
            LocalDate startDate,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr) {

        if (scenario == SimulationScenario.LIVE_OPEN_METEO) {
            throw new IllegalArgumentException("LIVE_OPEN_METEO must be fetched via OpenMeteoClient, not synthetic generator");
        }

        List<ForecastDayStats> days = new ArrayList<>(10);

        for (int dayIndex = 0; dayIndex < 10; dayIndex++) {
            LocalDate date = startDate.plusDays(dayIndex);
            ForecastDayStats stats = switch (scenario) {
                case CALM -> generateCalmDay(date, threshold2yr);
                case WATCH_2YR -> generateWatchDay(date, dayIndex, threshold2yr, threshold5yr, threshold20yr);
                case WARNING_5YR -> generateWarningDay(date, dayIndex, threshold2yr, threshold5yr, threshold20yr);
                case SEVERE_20YR -> generateSevereDay(date, dayIndex, threshold2yr, threshold5yr, threshold20yr);
                default -> generateCalmDay(date, threshold2yr);
            };
            days.add(stats);
        }

        return days;
    }

    private static ForecastDayStats generateCalmDay(LocalDate date, double t2) {
        double baseline = Math.min(25.0, t2 * 0.25);
        double qMin = round2(baseline * 0.7);
        double qP25 = round2(baseline * 0.85);
        double qMedian = round2(baseline);
        double qP75 = round2(baseline * 1.2);
        double qMax = round2(baseline * 1.4);
        return new ForecastDayStats(date, qMin, qP25, qMedian, qP75, qMax, 0.0, 0.0, 0.0);
    }

    private static ForecastDayStats generateWatchDay(
            LocalDate date, int dayIndex, double t2, double t5, double t20) {
        double s2, s5, s20;
        double scale;

        if (dayIndex == 3) {
            // Target crest at Day 3: S2 = 0.42, S5 = 0.05, S20 = 0.00
            s2 = 0.42;
            s5 = 0.05;
            s20 = 0.00;
            scale = 0.96;
        } else if (dayIndex == 2) {
            s2 = 0.30;
            s5 = 0.02;
            s20 = 0.00;
            scale = 0.85;
        } else if (dayIndex == 4) {
            s2 = 0.22;
            s5 = 0.01;
            s20 = 0.00;
            scale = 0.78;
        } else if (dayIndex == 1) {
            s2 = 0.12;
            s5 = 0.00;
            s20 = 0.00;
            scale = 0.65;
        } else if (dayIndex == 5) {
            s2 = 0.08;
            s5 = 0.00;
            s20 = 0.00;
            scale = 0.55;
        } else {
            s2 = 0.00;
            s5 = 0.00;
            s20 = 0.00;
            scale = 0.35;
        }

        double base = t2 * scale;
        double qMin = round2(base * 0.75);
        double qP25 = round2(base * 0.88);
        double qMedian = round2(base);
        double qP75 = round2(base * 1.08);
        double qMax = round2(base * 1.22);

        return new ForecastDayStats(date, qMin, qP25, qMedian, qP75, qMax, s2, s5, s20);
    }

    private static ForecastDayStats generateWarningDay(
            LocalDate date, int dayIndex, double t2, double t5, double t20) {
        double s2, s5, s20;
        double scale;

        if (dayIndex == 2) {
            // Target crest at Day 2: S2 = 0.95, S5 = 0.68, S20 = 0.15
            s2 = 0.95;
            s5 = 0.68;
            s20 = 0.15;
            scale = 1.12;
        } else if (dayIndex == 1) {
            s2 = 0.82;
            s5 = 0.45;
            s20 = 0.08;
            scale = 0.95;
        } else if (dayIndex == 3) {
            s2 = 0.70;
            s5 = 0.35;
            s20 = 0.05;
            scale = 0.88;
        } else if (dayIndex == 0) {
            s2 = 0.35;
            s5 = 0.08;
            s20 = 0.00;
            scale = 0.65;
        } else if (dayIndex == 4) {
            s2 = 0.40;
            s5 = 0.12;
            s20 = 0.00;
            scale = 0.70;
        } else if (dayIndex == 5) {
            s2 = 0.18;
            s5 = 0.02;
            s20 = 0.00;
            scale = 0.50;
        } else {
            s2 = 0.05;
            s5 = 0.00;
            s20 = 0.00;
            scale = 0.35;
        }

        double base = t5 * scale;
        double qMin = round2(base * 0.70);
        double qP25 = round2(base * 0.85);
        double qMedian = round2(base);
        double qP75 = round2(base * 1.15);
        double qMax = round2(base * 1.35);

        return new ForecastDayStats(date, qMin, qP25, qMedian, qP75, qMax, s2, s5, s20);
    }

    private static ForecastDayStats generateSevereDay(
            LocalDate date, int dayIndex, double t2, double t5, double t20) {
        double s2, s5, s20;
        double scale;

        if (dayIndex == 1 || dayIndex == 2) {
            // Target crest at Day 1-2: S2 = 1.00, S5 = 0.92, S20 = 0.78
            s2 = 1.00;
            s5 = 0.92;
            s20 = 0.78;
            scale = 1.25;
        } else if (dayIndex == 0) {
            s2 = 0.90;
            s5 = 0.70;
            s20 = 0.45;
            scale = 1.05;
        } else if (dayIndex == 3) {
            s2 = 0.85;
            s5 = 0.60;
            s20 = 0.30;
            scale = 0.95;
        } else if (dayIndex == 4) {
            s2 = 0.55;
            s5 = 0.25;
            s20 = 0.08;
            scale = 0.75;
        } else if (dayIndex == 5) {
            s2 = 0.30;
            s5 = 0.10;
            s20 = 0.00;
            scale = 0.55;
        } else {
            s2 = 0.12;
            s5 = 0.02;
            s20 = 0.00;
            scale = 0.40;
        }

        double base = t20 * scale;
        double qMin = round2(base * 0.72);
        double qP25 = round2(base * 0.86);
        double qMedian = round2(base);
        double qP75 = round2(base * 1.18);
        double qMax = round2(base * 1.45);

        return new ForecastDayStats(date, qMin, qP25, qMedian, qP75, qMax, s2, s5, s20);
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
