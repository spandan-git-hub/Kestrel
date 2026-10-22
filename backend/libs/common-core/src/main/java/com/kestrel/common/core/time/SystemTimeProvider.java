package com.kestrel.common.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class SystemTimeProvider implements TimeProvider {

    private final Clock clock;

    public SystemTimeProvider() {
        this(Clock.systemUTC());
    }

    public SystemTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Clock getClock() {
        return clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }

    @Override
    public LocalDate todayUtc() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
