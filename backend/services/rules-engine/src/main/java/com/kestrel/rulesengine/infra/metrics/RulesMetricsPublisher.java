package com.kestrel.rulesengine.infra.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Severity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes CloudWatch custom metrics for rules evaluation:
 * - kestrel.heat.alerts.total{severity}
 *
 * Automatically records metrics in Micrometer (Actuator) and emits standard
 * AWS CloudWatch Embedded Metric Format (EMF) logs for zero-latency CloudWatch ingestion.
 */
@Component
public class RulesMetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(RulesMetricsPublisher.class);
    private static final String NAMESPACE = "Kestrel";
    private static final String SERVICE = "rules-engine";

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, Counter> heatAlertCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> aqAlertCounters = new ConcurrentHashMap<>();

    public RulesMetricsPublisher(
            @Autowired(required = false) MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    public void recordHeatAlert(Severity severity) {
        if (severity == null) {
            return;
        }
        if (meterRegistry != null) {
            heatAlertCounters.computeIfAbsent(severity.name(), sev ->
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

    public double getHeatAlertCount(Severity severity) {
        Counter counter = heatAlertCounters.get(severity.name());
        return counter != null ? counter.count() : 0.0;
    }

    public void recordAqAlert(Severity severity) {
        if (severity == null) {
            return;
        }
        if (meterRegistry != null) {
            aqAlertCounters.computeIfAbsent(severity.name(), sev ->
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

    public double getAqAlertCount(Severity severity) {
        Counter counter = aqAlertCounters.get(severity.name());
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

