package com.kestrel.common.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class FixedTimeProvider implements TimeProvider {

    private final Clock clock;

    public FixedTimeProvider(Instant instant) {
        this.clock = Clock.fixed(instant, ZoneOffset.UTC);
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
