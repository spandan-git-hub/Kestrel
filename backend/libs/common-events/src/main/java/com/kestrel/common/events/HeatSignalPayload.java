package com.kestrel.common.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record HeatSignalPayload(
    String locationId,
    String runId,
    Instant fetchedAt,
    Instant dataAsOf,
    RegionType regionType,
    List<HeatDayStats> days
) {
    public record HeatDayStats(
        LocalDate date,
        double tmax,
        double apparentMax,
        double humidityMax,
        double normalTmax,
        double departure
    ) {}
}

