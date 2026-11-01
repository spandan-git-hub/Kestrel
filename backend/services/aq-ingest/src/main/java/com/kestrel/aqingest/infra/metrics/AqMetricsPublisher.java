package com.kestrel.aqingest.infra.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.Severity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes CloudWatch custom metrics for air quality ingestion:
 * - kestrel.aq.ingest.seconds (Timer / ingest duration in seconds)
 * - kestrel.aq.aqi.peak (Gauge / peak CPCB AQI value)
 * - kestrel.aq.alerts.total{severity} (Counter / alert counts by severity)
 *
 * Automatically records metrics in Micrometer (Actuator) and emits standard
 * AWS CloudWatch Embedded Metric Format (EMF) logs for zero-latency CloudWatch ingestion.
 */
@Component
public class AqMetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(AqMetricsPublisher.class);
    private static final String NAMESPACE = "Kestrel";
    private static final String SERVICE = "aq-ingest";

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final AtomicReference<Double> peakAqiGauge = new AtomicReference<>(0.0);
    private final Map<String, Counter> alertCounters = new ConcurrentHashMap<>();

    public AqMetricsPublisher(
            @Autowired(required = false) MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;

        if (meterRegistry != null) {
            Gauge.builder("kestrel.aq.aqi.peak", peakAqiGauge, AtomicReference::get)
                    .description("Peak CPCB AQI value across Indian stations")
                    .register(meterRegistry);
        }
    }

    public void recordIngestDuration(double durationSeconds) {
        if (meterRegistry != null) {
            Timer.builder("kestrel.aq.ingest.seconds")
                    .description("Duration of air quality forecast ingestion")
                    .register(meterRegistry)
                    .record(Duration.ofMillis((long) (durationSeconds * 1000)));
        }
        emitCloudWatchEmf(
                Map.of("kestrel.aq.ingest.seconds", durationSeconds),
                Map.of("kestrel.aq.ingest.seconds", "Seconds"),
                Map.of("Service", SERVICE, "Hazard", "AIR_QUALITY")
        );
    }

    public void recordPeakAqi(double peakAqi) {
        peakAqiGauge.set(peakAqi);
        emitCloudWatchEmf(
                Map.of("kestrel.aq.aqi.peak", peakAqi),
                Map.of("kestrel.aq.aqi.peak", "None"),
                Map.of("Service", SERVICE, "Hazard", "AIR_QUALITY")
        );
    }

    public void recordAlert(Severity severity) {
        if (severity == null) {
            return;
        }
        if (meterRegistry != null) {
            alertCounters.computeIfAbsent(severity.name(), sev ->
                    Counter.builder("kestrel.aq.alerts.total")
                            .tag("severity", sev)
                            .description("Total air quality alerts evaluated or published")
                            .register(meterRegistry)
            ).increment();
        }
        emitCloudWatchEmf(
                Map.of("kestrel.aq.alerts.total", 1.0),
                Map.of("kestrel.aq.alerts.total", "Count"),
                Map.of("Service", SERVICE, "Hazard", "AIR_QUALITY", "severity", severity.name())
        );
    }

    public double getPeakAqi() {
        return peakAqiGauge.get();
    }

    public double getAlertCount(Severity severity) {
        Counter counter = alertCounters.get(severity.name());
        return counter != null ? counter.count() : 0.0;
    }

    private void emitCloudWatchEmf(Map<String, Double> metrics, Map<String, String> units, Map<String, String> dimensions) {
        try {
            Map<String, Object> emf = new LinkedHashMap<>();
            Map<String, Object> aws = new LinkedHashMap<>();
            aws.put("Timestamp", Instant.now().toEpochMilli());

            List<Map<String, Object>> metricDefs = metrics.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("Name", e.getKey());
                        m.put("Unit", units.getOrDefault(e.getKey(), "None"));
                        return m;
                    })
                    .toList();

            List<String> dimensionKeys = new ArrayList<>(dimensions.keySet());
            Map<String, Object> cwm = new LinkedHashMap<>();
            cwm.put("Namespace", NAMESPACE);
            cwm.put("Dimensions", List.of(dimensionKeys));
            cwm.put("Metrics", metricDefs);

            aws.put("CloudWatchMetrics", List.of(cwm));
            emf.put("_aws", aws);

            dimensions.forEach(emf::put);
            metrics.forEach(emf::put);

            log.info("{}", objectMapper.writeValueAsString(emf));
        } catch (Exception e) {
            log.debug("Failed to serialize CloudWatch EMF metric: {}", e.getMessage());
        }
    }
}

