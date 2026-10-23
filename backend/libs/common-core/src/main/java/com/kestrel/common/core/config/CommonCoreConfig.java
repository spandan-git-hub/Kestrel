package com.kestrel.common.core.config;

import com.kestrel.common.core.time.SystemTimeProvider;
import com.kestrel.common.core.time.TimeProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CommonCoreConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public TimeProvider timeProvider(Clock clock) {
        return new SystemTimeProvider(clock);
    }
}
