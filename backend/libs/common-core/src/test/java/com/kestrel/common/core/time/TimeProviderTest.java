package com.kestrel.common.core.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TimeProviderTest {

    @Test
    void testFixedTimeProvider() {
        Instant fixedInstant = Instant.parse("2026-10-22T15:30:00Z");
        TimeProvider provider = TimeProvider.fixed(fixedInstant);

        assertThat(provider.now()).isEqualTo(fixedInstant);
        assertThat(provider.todayUtc()).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    @Test
    void testSystemTimeProvider() {
        TimeProvider provider = TimeProvider.system();
        assertThat(provider.now()).isNotNull();
        assertThat(provider.todayUtc()).isNotNull();
    }
}
