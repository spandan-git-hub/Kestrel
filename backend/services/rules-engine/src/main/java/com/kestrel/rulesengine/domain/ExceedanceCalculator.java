package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.Severity;

import java.time.LocalDate;
import java.util.List;

/**
 * Production Exceedance Share Calculator evaluating real ensemble forecasts.
 * For each day in the 10-day lookahead horizon:
 *   share_k(d) = #{members with q_i(d) > T_k} / memberCount
 * Triggers exceedance when share_k(d) >= 0.30.
 */
public class ExceedanceCalculator {

    public static final double DEFAULT_CONSENSUS_THRESHOLD = 0.30;
    public static final int DEFAULT_HORIZON_DAYS = 10;

    /**
     * Calculates the statistical exceedance share for ensemble member discharges:
     *   share_k(d) = #{members with q_i(d) > T_k} / memberCount
     */
    public static double calculateShare(List<Double> memberDischarges, double threshold) {
        if (memberDischarges == null || memberDischarges.isEmpty()) {
            return 0.0;
        }
        long exceedingCount = memberDischarges.stream().filter(q -> q > threshold).count();
        return Math.round(((double) exceedingCount / memberDischarges.size()) * 100.0) / 100.0;
    }

    /**
     * Calculates exceedance share given member count and exceeding count.
     */
    public static double calculateShare(int memberCount, int exceedingCount) {
        if (memberCount <= 0) {
            return 0.0;
        }
        return Math.round(((double) exceedingCount / memberCount) * 100.0) / 100.0;
    }

    /**
     * Checks if calculated exceedance share meets or exceeds the consensus threshold (default 30%).
     */
    public static boolean isExceeded(double share) {
        return isExceeded(share, DEFAULT_CONSENSUS_THRESHOLD);
    }

    public static boolean isExceeded(double share, double threshold) {
        return share >= threshold;
    }

    public ExceedanceEvaluation evaluate(List<ForecastDayStats> days) {
        return evaluate(days, DEFAULT_HORIZON_DAYS, DEFAULT_CONSENSUS_THRESHOLD);
    }

    public ExceedanceEvaluation evaluate(List<ForecastDayStats> days, int horizonDays, double threshold) {
        if (days == null || days.isEmpty()) {
            return new ExceedanceEvaluation(Severity.NONE, null, 0.0);
        }

        int limit = Math.min(days.size(), horizonDays);
        List<ForecastDayStats> window = days.subList(0, limit);

        // 1. Check SEVERE (20-year return period threshold T_20)
        LocalDate firstSevereDate = null;
        double peakSevereShare = 0.0;
        for (ForecastDayStats day : window) {
            double share = day.share20yr();
            if (share > peakSevereShare) {
                peakSevereShare = share;
            }
            if (isExceeded(share, threshold) && firstSevereDate == null) {
                firstSevereDate = day.date();
            }
        }
        if (firstSevereDate != null) {
            return new ExceedanceEvaluation(Severity.SEVERE, firstSevereDate, peakSevereShare);
        }

        // 2. Check WARNING (5-year return period threshold T_5)
        LocalDate firstWarningDate = null;
        double peakWarningShare = 0.0;
        for (ForecastDayStats day : window) {
            double share = day.share5yr();
            if (share > peakWarningShare) {
                peakWarningShare = share;
            }
            if (isExceeded(share, threshold) && firstWarningDate == null) {
                firstWarningDate = day.date();
            }
        }
        if (firstWarningDate != null) {
            return new ExceedanceEvaluation(Severity.WARNING, firstWarningDate, peakWarningShare);
        }

        // 3. Check WATCH (2-year return period threshold T_2)
        LocalDate firstWatchDate = null;
        double peakWatchShare = 0.0;
        for (ForecastDayStats day : window) {
            double share = day.share2yr();
            if (share > peakWatchShare) {
                peakWatchShare = share;
            }
            if (isExceeded(share, threshold) && firstWatchDate == null) {
                firstWatchDate = day.date();
            }
        }
        if (firstWatchDate != null) {
            return new ExceedanceEvaluation(Severity.WATCH, firstWatchDate, peakWatchShare);
        }

        return new ExceedanceEvaluation(Severity.NONE, null, 0.0);
    }
}
