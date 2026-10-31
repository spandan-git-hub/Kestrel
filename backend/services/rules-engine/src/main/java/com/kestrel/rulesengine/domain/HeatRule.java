package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.HeatSignalPayload;
import com.kestrel.common.events.HeatSignalPayload.HeatDayStats;
import com.kestrel.common.events.RegionType;
import com.kestrel.common.events.Severity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Pure domain HeatRule enforcing official India Meteorological Department (IMD) heat-wave standards:
 *
 * 1. Regional Temperature Gate:
 *    - PLAINS:  Tmax >= 40.0°C
 *    - COASTAL: Tmax >= 37.0°C
 *    - HILLY:   Tmax >= 30.0°C
 *
 * 2. Daily Heat-Wave Classification:
 *    - Heat-Wave Day: Gate met AND (departure >= +4.5°C OR Tmax >= 45.0°C)
 *    - Severe Heat-Wave Day: Gate met AND (departure > +6.4°C OR Tmax >= 47.0°C)
 *
 * 3. Persistence Criteria across Forecast Horizon:
 *    - WATCH:   >= 1 Heat-Wave Day predicted.
 *    - WARNING: >= 2 consecutive Heat-Wave Days predicted.
 *    - SEVERE:  >= 2 consecutive Severe Heat-Wave Days predicted.
 */
public class HeatRule {

    public static final double GATE_PLAINS = 40.0;
    public static final double GATE_COASTAL = 37.0;
    public static final double GATE_HILLY = 30.0;

    public static final double DEPARTURE_HEAT_WAVE = 4.5;
    public static final double DEPARTURE_SEVERE_HEAT_WAVE = 6.4;

    public static final double TMAX_HEAT_WAVE = 45.0;
    public static final double TMAX_SEVERE_HEAT_WAVE = 47.0;

    public static double getRegionalGate(RegionType regionType) {
        if (regionType == null) {
            return GATE_PLAINS;
        }
        return switch (regionType) {
            case COASTAL -> GATE_COASTAL;
            case HILLY -> GATE_HILLY;
            case PLAINS -> GATE_PLAINS;
        };
    }

    public static boolean isGateMet(double tmax, RegionType regionType) {
        return tmax >= getRegionalGate(regionType);
    }

    public static boolean isSevereHeatWaveDay(double tmax, double departure, RegionType regionType) {
        if (!isGateMet(tmax, regionType)) {
            return false;
        }
        return departure > DEPARTURE_SEVERE_HEAT_WAVE || tmax >= TMAX_SEVERE_HEAT_WAVE;
    }

    public static boolean isHeatWaveDay(double tmax, double departure, RegionType regionType) {
        if (!isGateMet(tmax, regionType)) {
            return false;
        }
        if (isSevereHeatWaveDay(tmax, departure, regionType)) {
            return true;
        }
        return departure >= DEPARTURE_HEAT_WAVE || tmax >= TMAX_HEAT_WAVE;
    }

    public HeatEvaluation evaluate(HeatSignalPayload payload) {
        if (payload == null || payload.days() == null || payload.days().isEmpty()) {
            return new HeatEvaluation(Severity.NONE, null, 0.0, 0.0, 0, 0);
        }
        return evaluate(payload.regionType(), payload.days());
    }

    public HeatEvaluation evaluate(RegionType regionType, List<HeatDayStats> days) {
        if (days == null || days.isEmpty()) {
            return new HeatEvaluation(Severity.NONE, null, 0.0, 0.0, 0, 0);
        }

        List<HeatDayStats> sortedDays = days.stream()
                .sorted(Comparator.comparing(HeatDayStats::date))
                .toList();

        double peakDeparture = Double.NEGATIVE_INFINITY;
        double peakTmax = Double.NEGATIVE_INFINITY;
        int heatWaveDaysCount = 0;
        int severeHeatWaveDaysCount = 0;
        LocalDate firstHeatWaveDate = null;
        LocalDate firstSevereHeatWaveDate = null;

        boolean[] isHw = new boolean[sortedDays.size()];
        boolean[] isSevere = new boolean[sortedDays.size()];

        for (int i = 0; i < sortedDays.size(); i++) {
            HeatDayStats day = sortedDays.get(i);
            peakDeparture = Math.max(peakDeparture, day.departure());
            peakTmax = Math.max(peakTmax, day.tmax());

            boolean severe = isSevereHeatWaveDay(day.tmax(), day.departure(), regionType);
            boolean hw = isHeatWaveDay(day.tmax(), day.departure(), regionType);

            isSevere[i] = severe;
            isHw[i] = hw;

            if (severe) {
                severeHeatWaveDaysCount++;
                if (firstSevereHeatWaveDate == null) {
                    firstSevereHeatWaveDate = day.date();
                }
            }
            if (hw) {
                heatWaveDaysCount++;
                if (firstHeatWaveDate == null) {
                    firstHeatWaveDate = day.date();
                }
            }
        }

        if (peakDeparture == Double.NEGATIVE_INFINITY) peakDeparture = 0.0;
        if (peakTmax == Double.NEGATIVE_INFINITY) peakTmax = 0.0;

        // 1. Check for SEVERE: >= 2 consecutive Severe Heat-Wave Days
        LocalDate severeConsecutiveStart = null;
        for (int i = 0; i < sortedDays.size() - 1; i++) {
            if (isSevere[i] && isSevere[i + 1]) {
                LocalDate d1 = sortedDays.get(i).date();
                LocalDate d2 = sortedDays.get(i + 1).date();
                if (d2.equals(d1.plusDays(1))) {
                    severeConsecutiveStart = d1;
                    break;
                }
            }
        }
        if (severeConsecutiveStart != null) {
            return new HeatEvaluation(
                    Severity.SEVERE,
                    severeConsecutiveStart,
                    roundOneDecimal(peakDeparture),
                    roundOneDecimal(peakTmax),
                    heatWaveDaysCount,
                    severeHeatWaveDaysCount
            );
        }

        // 2. Check for WARNING: >= 2 consecutive Heat-Wave Days
        LocalDate warningConsecutiveStart = null;
        for (int i = 0; i < sortedDays.size() - 1; i++) {
            if (isHw[i] && isHw[i + 1]) {
                LocalDate d1 = sortedDays.get(i).date();
                LocalDate d2 = sortedDays.get(i + 1).date();
                if (d2.equals(d1.plusDays(1))) {
                    warningConsecutiveStart = d1;
                    break;
                }
            }
        }
        if (warningConsecutiveStart != null) {
            return new HeatEvaluation(
                    Severity.WARNING,
                    warningConsecutiveStart,
                    roundOneDecimal(peakDeparture),
                    roundOneDecimal(peakTmax),
                    heatWaveDaysCount,
                    severeHeatWaveDaysCount
            );
        }

        // 3. Check for WATCH: >= 1 Heat-Wave Day
        if (heatWaveDaysCount >= 1) {
            return new HeatEvaluation(
                    Severity.WATCH,
                    firstHeatWaveDate,
                    roundOneDecimal(peakDeparture),
                    roundOneDecimal(peakTmax),
                    heatWaveDaysCount,
                    severeHeatWaveDaysCount
            );
        }

        // 4. Default NONE
        return new HeatEvaluation(
                Severity.NONE,
                null,
                roundOneDecimal(peakDeparture),
                roundOneDecimal(peakTmax),
                0,
                0
        );
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

