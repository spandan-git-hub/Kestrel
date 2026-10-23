package com.kestrel.rulesengine.api.dto;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;

import java.util.List;

public record LocationForecastResponse(
        String locationId,
        Thresholds thresholds,
        List<ForecastDayStats> days
) {
    public record Thresholds(double rp2, double rp5, double rp20) {}
}
