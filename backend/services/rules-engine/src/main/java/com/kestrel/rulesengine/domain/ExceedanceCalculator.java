package com.kestrel.rulesengine.domain;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.Severity;

import java.time.LocalDate;
import java.util.List;

public class ExceedanceCalculator {

    public static final double DEFAULT_CONSENSUS_THRESHOLD = 0.30;
    public static final int DEFAULT_HORIZON_DAYS = 7;

    public ExceedanceEvaluation evaluate(List<ForecastDayStats> days) {
        return evaluate(days, DEFAULT_HORIZON_DAYS, DEFAULT_CONSENSUS_THRESHOLD);
    }

    public ExceedanceEvaluation evaluate(List<ForecastDayStats> days, int horizonDays, double threshold) {
        if (days == null || days.isEmpty()) {
            return new ExceedanceEvaluation(Severity.NONE, null, 0.0);
        }

        int limit = Math.min(days.size(), horizonDays);
        List<ForecastDayStats> window = days.subList(0, limit);

        // 1. Check SEVERE (20-year return period threshold)
        LocalDate firstSevereDate = null;
        double peakSevereShare = 0.0;
        for (ForecastDayStats day : window) {
            double share = day.share20yr();
            if (share > peakSevereShare) {
                peakSevereShare = share;
            }
            if (share >= threshold && firstSevereDate == null) {
                firstSevereDate = day.date();
            }
        }
        if (firstSevereDate != null) {
            return new ExceedanceEvaluation(Severity.SEVERE, firstSevereDate, peakSevereShare);
        }

        // 2. Check WARNING (5-year return period threshold)
        LocalDate firstWarningDate = null;
        double peakWarningShare = 0.0;
        for (ForecastDayStats day : window) {
            double share = day.share5yr();
            if (share > peakWarningShare) {
                peakWarningShare = share;
            }
            if (share >= threshold && firstWarningDate == null) {
                firstWarningDate = day.date();
            }
        }
        if (firstWarningDate != null) {
            return new ExceedanceEvaluation(Severity.WARNING, firstWarningDate, peakWarningShare);
        }

        // 3. Check WATCH (2-year return period threshold)
        LocalDate firstWatchDate = null;
        double peakWatchShare = 0.0;
        for (ForecastDayStats day : window) {
            double share = day.share2yr();
            if (share > peakWatchShare) {
                peakWatchShare = share;
            }
            if (share >= threshold && firstWatchDate == null) {
                firstWatchDate = day.date();
            }
        }
        if (firstWatchDate != null) {
            return new ExceedanceEvaluation(Severity.WATCH, firstWatchDate, peakWatchShare);
        }

        return new ExceedanceEvaluation(Severity.NONE, null, 0.0);
    }
}
