package com.kestrel.rulesengine.infra.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.Severity;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RulesMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private RulesMetricsPublisher publisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        publisher = new RulesMetricsPublisher(meterRegistry, objectMapper);
    }

    @Test
    @DisplayName("Publishes kestrel.heat.alerts.total counter metric tagged by severity")
    void recordsHeatAlertsBySeverity() {
        publisher.recordHeatAlert(Severity.WATCH);
        publisher.recordHeatAlert(Severity.WARNING);
        publisher.recordHeatAlert(Severity.WARNING);
        publisher.recordHeatAlert(Severity.SEVERE);

        assertThat(publisher.getHeatAlertCount(Severity.WATCH)).isEqualTo(1.0);
        assertThat(publisher.getHeatAlertCount(Severity.WARNING)).isEqualTo(2.0);
        assertThat(publisher.getHeatAlertCount(Severity.SEVERE)).isEqualTo(1.0);

        assertThat(meterRegistry.find("kestrel.heat.alerts.total").tag("severity", "WARNING").counter()).isNotNull();
        assertThat(meterRegistry.find("kestrel.heat.alerts.total").tag("severity", "WARNING").counter().count()).isEqualTo(2.0);
    }
}

