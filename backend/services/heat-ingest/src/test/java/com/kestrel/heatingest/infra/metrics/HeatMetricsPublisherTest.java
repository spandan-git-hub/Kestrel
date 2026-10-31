package com.kestrel.heatingest.infra.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.Severity;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeatMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private HeatMetricsPublisher publisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        publisher = new HeatMetricsPublisher(meterRegistry, objectMapper);
    }

    @Test
    @DisplayName("Publishes kestrel.heat.ingest.seconds timer metric")
    void recordsIngestDuration() {
        publisher.recordIngestDuration(1.25);

        assertThat(meterRegistry.find("kestrel.heat.ingest.seconds").timer()).isNotNull();
        assertThat(meterRegistry.find("kestrel.heat.ingest.seconds").timer().count()).isEqualTo(1L);
        assertThat(meterRegistry.find("kestrel.heat.ingest.seconds").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isGreaterThanOrEqualTo(1200.0);
    }

    @Test
    @DisplayName("Publishes kestrel.heat.departures.peak gauge metric")
    void recordsPeakDeparture() {
        publisher.recordPeakDeparture(6.8);

        assertThat(publisher.getPeakDeparture()).isEqualTo(6.8);
        assertThat(meterRegistry.find("kestrel.heat.departures.peak").gauge()).isNotNull();
        assertThat(meterRegistry.find("kestrel.heat.departures.peak").gauge().value()).isEqualTo(6.8);

        publisher.recordPeakDeparture(4.2);
        assertThat(publisher.getPeakDeparture()).isEqualTo(4.2);
        assertThat(meterRegistry.find("kestrel.heat.departures.peak").gauge().value()).isEqualTo(4.2);
    }

    @Test
    @DisplayName("Publishes kestrel.heat.alerts.total counter metric tagged by severity")
    void recordsHeatAlertsBySeverity() {
        publisher.recordAlert(Severity.WATCH);
        publisher.recordAlert(Severity.WARNING);
        publisher.recordAlert(Severity.WARNING);
        publisher.recordAlert(Severity.SEVERE);

        assertThat(publisher.getAlertCount(Severity.WATCH)).isEqualTo(1.0);
        assertThat(publisher.getAlertCount(Severity.WARNING)).isEqualTo(2.0);
        assertThat(publisher.getAlertCount(Severity.SEVERE)).isEqualTo(1.0);

        assertThat(meterRegistry.find("kestrel.heat.alerts.total").tag("severity", "WARNING").counter()).isNotNull();
        assertThat(meterRegistry.find("kestrel.heat.alerts.total").tag("severity", "WARNING").counter().count()).isEqualTo(2.0);
    }
}

