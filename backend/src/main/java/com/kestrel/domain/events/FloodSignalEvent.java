package com.kestrel.domain.events;

import com.kestrel.domain.ForecastDayStats;
import java.time.Instant;
import java.util.List;

public record FloodSignalEvent(
    String locationId,
    Instant dataAsOf,
    boolean isSimulated,
    List<ForecastDayStats> days
) {}
