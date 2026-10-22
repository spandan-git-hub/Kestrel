package com.kestrel.floodingest.domain;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExceedanceCalculator {

    public static ForecastDayStats calculateDayStats(
            LocalDate date,
            List<Double> memberValues,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr) {
        Objects.requireNonNull(date, "date cannot be null");
        if (memberValues == null || memberValues.isEmpty()) {
            return new ForecastDayStats(date, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        List<Double> sorted = new ArrayList<>(memberValues);
        Collections.sort(sorted);
        int totalMembers = sorted.size();

        double qMin = round2(sorted.get(0));
        double qP25 = round2(percentile(sorted, 0.25));
        double qMedian = round2(percentile(sorted, 0.50));
        double qP75 = round2(percentile(sorted, 0.75));
        double qMax = round2(sorted.get(totalMembers - 1));

        long count2yr = sorted.stream().filter(v -> v >= threshold2yr).count();
        long count5yr = sorted.stream().filter(v -> v >= threshold5yr).count();
        long count20yr = sorted.stream().filter(v -> v >= threshold20yr).count();

        double share2yr = round2((double) count2yr / totalMembers);
        double share5yr = round2((double) count5yr / totalMembers);
        double share20yr = round2((double) count20yr / totalMembers);

        return new ForecastDayStats(date, qMin, qP25, qMedian, qP75, qMax, share2yr, share5yr, share20yr);
    }

    public static List<ForecastDayStats> calculateForecast(
            List<LocalDate> dates,
            Map<String, List<Double>> memberDischarges,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr) {
        if (dates == null || dates.isEmpty() || memberDischarges == null || memberDischarges.isEmpty()) {
            return Collections.emptyList();
        }

        List<ForecastDayStats> result = new ArrayList<>();
        int dayCount = dates.size();

        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            LocalDate date = dates.get(dayIndex);
            List<Double> dailyMemberValues = new ArrayList<>();

            for (List<Double> values : memberDischarges.values()) {
                if (values != null && dayIndex < values.size() && values.get(dayIndex) != null) {
                    dailyMemberValues.add(values.get(dayIndex));
                }
            }

            result.add(calculateDayStats(date, dailyMemberValues, threshold2yr, threshold5yr, threshold20yr));
        }

        return result;
    }

    private static double percentile(List<Double> sorted, double p) {
        int n = sorted.size();
        if (n == 1) {
            return sorted.get(0);
        }
        double index = p * (n - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = index - lower;
        return sorted.get(lower) + weight * (sorted.get(upper) - sorted.get(lower));
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
