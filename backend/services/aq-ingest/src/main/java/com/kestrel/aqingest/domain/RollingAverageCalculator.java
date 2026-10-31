package com.kestrel.aqingest.domain;

import com.kestrel.common.events.AqSignalPayload;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RollingAverageCalculator {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter ISO_LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public record RawHourlyAq(
            String time,
            Double pm10,
            Double pm25,
            Double no2,
            Double so2,
            Double o3,
            Double coUgM3
    ) {
        public Double coMgM3() {
            return coUgM3 != null ? coUgM3 / 1000.0 : null;
        }
    }

    public record ProcessedHourlyAq(
            String time,
            Double pm10,
            Double pm25,
            Double no2,
            Double so2,
            Double o3,
            Double coMgm3,
            Double pm25_24h,
            Double pm10_24h,
            Double no2_24h,
            Double so2_24h,
            Double o3_8h,
            Double co_8h_mgm3,
            int validHours24
    ) {}

    public static List<ProcessedHourlyAq> processHourlyData(List<RawHourlyAq> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }

        List<RawHourlyAq> sorted = rawList.stream()
                .sorted(Comparator.comparing(RawHourlyAq::time))
                .toList();

        List<ProcessedHourlyAq> result = new ArrayList<>(sorted.size());

        for (int i = 0; i < sorted.size(); i++) {
            RawHourlyAq current = sorted.get(i);

            // 24-hour trailing window (indices max(0, i - 23) to i)
            int start24 = Math.max(0, i - 23);
            List<RawHourlyAq> window24 = sorted.subList(start24, i + 1);

            Double pm25_24h = calculateRollingMean(window24, RawHourlyAq::pm25, 16, 1);
            Double pm10_24h = calculateRollingMean(window24, RawHourlyAq::pm10, 16, 1);
            Double no2_24h = calculateRollingMean(window24, RawHourlyAq::no2, 16, 1);
            Double so2_24h = calculateRollingMean(window24, RawHourlyAq::so2, 16, 1);
            int validHours24 = (int) window24.stream().map(RawHourlyAq::pm25).filter(Objects::nonNull).count();

            // 8-hour trailing window (indices max(0, i - 7) to i)
            int start8 = Math.max(0, i - 7);
            List<RawHourlyAq> window8 = sorted.subList(start8, i + 1);

            Double o3_8h = calculateRollingMean(window8, RawHourlyAq::o3, 6, 1);
            Double co_8h_mgm3 = calculateRollingMean(window8, RawHourlyAq::coMgM3, 6, 2);

            result.add(new ProcessedHourlyAq(
                    current.time(),
                    current.pm10(),
                    current.pm25(),
                    current.no2(),
                    current.so2(),
                    current.o3(),
                    current.coMgM3() != null ? round(current.coMgM3(), 3) : null,
                    pm25_24h,
                    pm10_24h,
                    no2_24h,
                    so2_24h,
                    o3_8h,
                    co_8h_mgm3,
                    validHours24
            ));
        }

        return result;
    }

    public static AqSignalPayload.CurrentPollutants extractCurrentPollutants(
            List<ProcessedHourlyAq> processedList,
            LocalDateTime nowKolkata) {
        if (processedList == null || processedList.isEmpty()) {
            return new AqSignalPayload.CurrentPollutants(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0);
        }

        // Find latest hour <= nowKolkata
        ProcessedHourlyAq target = null;
        for (ProcessedHourlyAq item : processedList) {
            try {
                LocalDateTime itemTime = LocalDateTime.parse(item.time(), ISO_LOCAL_FORMATTER);
                if (!itemTime.isAfter(nowKolkata)) {
                    target = item;
                } else {
                    break;
                }
            } catch (Exception e) {
                // Ignore parse error and keep going
            }
        }

        if (target == null) {
            target = processedList.get(0);
        }

        return new AqSignalPayload.CurrentPollutants(
                target.pm25_24h() != null ? target.pm25_24h() : 0.0,
                target.pm10_24h() != null ? target.pm10_24h() : 0.0,
                target.no2_24h() != null ? target.no2_24h() : 0.0,
                target.so2_24h() != null ? target.so2_24h() : 0.0,
                target.o3_8h() != null ? target.o3_8h() : 0.0,
                target.co_8h_mgm3() != null ? target.co_8h_mgm3() : 0.0,
                target.validHours24()
        );
    }

    public static List<AqSignalPayload.ForecastDayPeak> extractForecastPeaks(
            List<ProcessedHourlyAq> processedList,
            LocalDate startDateInclusive) {
        if (processedList == null || processedList.isEmpty()) {
            return List.of();
        }

        // Group processed hourly entries by LocalDate
        Map<LocalDate, List<ProcessedHourlyAq>> grouped = processedList.stream()
                .filter(item -> {
                    try {
                        LocalDate d = LocalDate.parse(item.time().substring(0, 10));
                        return !d.isBefore(startDateInclusive);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(item -> LocalDate.parse(item.time().substring(0, 10))));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<ProcessedHourlyAq> hours = entry.getValue();

                    double peakPm25 = hours.stream()
                            .map(ProcessedHourlyAq::pm25_24h)
                            .filter(Objects::nonNull)
                            .max(Double::compareTo)
                            .orElse(0.0);

                    double peakPm10 = hours.stream()
                            .map(ProcessedHourlyAq::pm10_24h)
                            .filter(Objects::nonNull)
                            .max(Double::compareTo)
                            .orElse(0.0);

                    double peakNo2 = hours.stream()
                            .map(ProcessedHourlyAq::no2_24h)
                            .filter(Objects::nonNull)
                            .max(Double::compareTo)
                            .orElse(0.0);

                    double peakSo2 = hours.stream()
                            .map(ProcessedHourlyAq::so2_24h)
                            .filter(Objects::nonNull)
                            .max(Double::compareTo)
                            .orElse(0.0);

                    double peakO3 = hours.stream()
                            .map(ProcessedHourlyAq::o3_8h)
                            .filter(Objects::nonNull)
                            .max(Double::compareTo)
                            .orElse(0.0);

                    double peakCo = hours.stream()
                            .map(ProcessedHourlyAq::co_8h_mgm3)
                            .filter(Objects::nonNull)
                            .max(Double::compareTo)
                            .orElse(0.0);

                    return new AqSignalPayload.ForecastDayPeak(
                            date,
                            round(peakPm25, 1),
                            round(peakPm10, 1),
                            round(peakNo2, 1),
                            round(peakSo2, 1),
                            round(peakO3, 1),
                            round(peakCo, 2)
                    );
                })
                .toList();
    }

    private static Double calculateRollingMean(
            List<RawHourlyAq> window,
            java.util.function.Function<RawHourlyAq, Double> extractor,
            int minValidHours,
            int decimalPlaces) {
        List<Double> validValues = window.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .filter(d -> !d.isNaN())
                .toList();

        if (validValues.size() < minValidHours) {
            return null;
        }

        double sum = 0.0;
        for (Double val : validValues) {
            sum += val;
        }
        double mean = sum / validValues.size();
        return round(mean, decimalPlaces);
    }

    public static double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}

