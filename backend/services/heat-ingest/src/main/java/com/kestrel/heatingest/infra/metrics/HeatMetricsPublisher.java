package com.kestrel.heatingest.infra.metrics;

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
 * Publishes CloudWatch custom metrics for heat ingestion:
 * - kestrel.heat.ingest.seconds (Timer / ingest duration in seconds)
 * - kestrel.heat.departures.peak (Gauge / peak departure value)
 * - kestrel.heat.alerts.total{severity} (Counter / alert counts by severity)
 *
 * Automatically records metrics in Micrometer (Actuator) and emits standard
 * AWS CloudWatch Embedded Metric Format (EMF) logs for zero-latency CloudWatch ingestion.
 */
@Component
public class HeatMetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(HeatMetricsPublisher.class);
    private static final String NAMESPACE = "Kestrel";
    private static final String SERVICE = "heat-ingest";

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final AtomicReference<Double> peakDepartureGauge = new AtomicReference<>(0.0);
    private final Map<String, Counter> alertCounters = new ConcurrentHashMap<>();

    public HeatMetricsPublisher(
            @Autowired(required = false) MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;

        if (meterRegistry != null) {
            Gauge.builder("kestrel.heat.departures.peak", peakDepartureGauge, AtomicReference::get)
                    .description("Peak temperature departure across forecast horizon")
                    .register(meterRegistry);
        }
    }

    public void recordIngestDuration(double durationSeconds) {
        if (meterRegistry != null) {
            Timer.builder("kestrel.heat.ingest.seconds")
                    .description("Duration of heat forecast ingestion")
                    .register(meterRegistry)
                    .record(Duration.ofMillis((long) (durationSeconds * 1000)));
        }
        emitCloudWatchEmf(
                Map.of("kestrel.heat.ingest.seconds", durationSeconds),
                Map.of("kestrel.heat.ingest.seconds", "Seconds"),
                Map.of("Service", SERVICE, "Hazard", "HEAT")
        );
    }

    public void recordPeakDeparture(double peakDeparture) {
        peakDepartureGauge.set(peakDeparture);
        emitCloudWatchEmf(
                Map.of("kestrel.heat.departures.peak", peakDeparture),
                Map.of("kestrel.heat.departures.peak", "None"),
                Map.of("Service", SERVICE, "Hazard", "HEAT")
        );
    }

    public void recordAlert(Severity severity) {
        if (severity == null) {
            return;
        }
        if (meterRegistry != null) {
            alertCounters.computeIfAbsent(severity.name(), sev ->
                    Counter.builder("kestrel.heat.alerts.total")
                            .tag("severity", sev)
                            .description("Total heat alerts evaluated or published")
                            .register(meterRegistry)
            ).increment();
        }
        emitCloudWatchEmf(
                Map.of("kestrel.heat.alerts.total", 1.0),
                Map.of("kestrel.heat.alerts.total", "Count"),
                Map.of("Service", SERVICE, "Hazard", "HEAT", "severity", severity.name())
        );
    }

    public double getPeakDeparture() {
        return peakDepartureGauge.get();
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

            // Put dimensions and metric values as root properties for EMF
            dimensions.forEach(emf::put);
            metrics.forEach(emf::put);

            log.info("{}", objectMapper.writeValueAsString(emf));
        } catch (Exception e) {
            log.debug("Failed to serialize CloudWatch EMF metric: {}", e.getMessage());
        }
    }
}

