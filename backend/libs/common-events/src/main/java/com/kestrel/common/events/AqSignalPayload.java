package com.kestrel.common.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AqSignalPayload(
    String locationId,
    String runId,
    Instant fetchedAt,
    Instant dataAsOf,
    CurrentPollutants current,
    List<ForecastDayPeak> forecastPeaks
) {
    public record CurrentPollutants(
        double pm25_24h,
        double pm10_24h,
        double no2_24h,
        double so2_24h,
        double o3_8h,
        double co_8h_mgm3,
        int validHours
    ) {}

    public record ForecastDayPeak(
        LocalDate date,
        double pm25_24h,
        double pm10_24h,
        double no2_24h,
        double so2_24h,
        double o3_8h,
        double co_8h_mgm3
    ) {}
}

