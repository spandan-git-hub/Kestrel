package com.kestrel.aqingest.infra.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.Severity;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AqMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private AqMetricsPublisher publisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        publisher = new AqMetricsPublisher(meterRegistry, objectMapper);
    }

    @Test
    @DisplayName("Publishes kestrel.aq.ingest.seconds timer metric")
    void recordsIngestDuration() {
        publisher.recordIngestDuration(1.42);

        assertThat(meterRegistry.find("kestrel.aq.ingest.seconds").timer()).isNotNull();
        assertThat(meterRegistry.find("kestrel.aq.ingest.seconds").timer().count()).isEqualTo(1L);
        assertThat(meterRegistry.find("kestrel.aq.ingest.seconds").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isGreaterThanOrEqualTo(1400.0);
    }

    @Test
    @DisplayName("Publishes kestrel.aq.aqi.peak gauge metric")
    void recordsPeakAqi() {
        publisher.recordPeakAqi(323.0);

        assertThat(publisher.getPeakAqi()).isEqualTo(323.0);
        assertThat(meterRegistry.find("kestrel.aq.aqi.peak").gauge()).isNotNull();
        assertThat(meterRegistry.find("kestrel.aq.aqi.peak").gauge().value()).isEqualTo(323.0);

        publisher.recordPeakAqi(450.0);
        assertThat(publisher.getPeakAqi()).isEqualTo(450.0);
        assertThat(meterRegistry.find("kestrel.aq.aqi.peak").gauge().value()).isEqualTo(450.0);
    }

    @Test
    @DisplayName("Publishes kestrel.aq.alerts.total counter metric tagged by severity")
    void recordsAqAlertsBySeverity() {
        publisher.recordAlert(Severity.WATCH);
        publisher.recordAlert(Severity.WARNING);
        publisher.recordAlert(Severity.WARNING);
        publisher.recordAlert(Severity.SEVERE);

        assertThat(publisher.getAlertCount(Severity.WATCH)).isEqualTo(1.0);
        assertThat(publisher.getAlertCount(Severity.WARNING)).isEqualTo(2.0);
        assertThat(publisher.getAlertCount(Severity.SEVERE)).isEqualTo(1.0);

        assertThat(meterRegistry.find("kestrel.aq.alerts.total").tag("severity", "WARNING").counter()).isNotNull();
        assertThat(meterRegistry.find("kestrel.aq.alerts.total").tag("severity", "WARNING").counter().count()).isEqualTo(2.0);
    }
}

