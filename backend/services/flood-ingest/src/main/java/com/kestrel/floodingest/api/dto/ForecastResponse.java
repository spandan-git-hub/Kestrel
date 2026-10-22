package com.kestrel.floodingest.api.dto;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import java.util.List;

public record ForecastResponse(
    String locationId,
    ThresholdsDto thresholds,
    List<ForecastDayStats> days
) {
    public record ThresholdsDto(double rp2, double rp5, double rp20) {}
}
