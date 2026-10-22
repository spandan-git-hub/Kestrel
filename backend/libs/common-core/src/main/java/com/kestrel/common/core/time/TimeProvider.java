package com.kestrel.common.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public interface TimeProvider {
    Clock getClock();
    Instant now();
    LocalDate todayUtc();

    static TimeProvider system() {
        return new SystemTimeProvider();
    }

    static TimeProvider fixed(Instant instant) {
        return new FixedTimeProvider(instant);
    }
}
