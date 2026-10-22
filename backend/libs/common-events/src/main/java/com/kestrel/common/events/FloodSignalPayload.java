package com.kestrel.common.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FloodSignalPayload(
    String locationId,
    Instant dataAsOf,
    boolean isSimulated,
    int memberCount,
    List<ForecastDayStats> days
) {
    public record ForecastDayStats(
        LocalDate date,
        double qMin,
        double qP25,
        double qMedian,
        double qP75,
        double qMax,
        double share2yr,
        double share5yr,
        double share20yr
    ) {}
}
