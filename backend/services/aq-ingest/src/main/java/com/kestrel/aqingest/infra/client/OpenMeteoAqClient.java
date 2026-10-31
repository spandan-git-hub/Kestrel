package com.kestrel.aqingest.infra.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.aqingest.domain.RollingAverageCalculator;
import com.kestrel.common.core.budget.CallBudgetTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OpenMeteoAqClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoAqClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CallBudgetTracker callBudgetTracker;

    public OpenMeteoAqClient(
            @Qualifier("openMeteoAqRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            CallBudgetTracker callBudgetTracker) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.callBudgetTracker = callBudgetTracker;
    }

    public Optional<AqForecastResult> fetchHourlyAq(double latitude, double longitude) {
        return fetchHourlyAq(latitude, longitude, 2, 3);
    }

    public Optional<AqForecastResult> fetchHourlyAq(double latitude, double longitude, int pastDays, int forecastDays) {
        // Enforce DynamoDB atomic call budget (< 500 req/min, < 8000 req/day) shared across ingest services
        if (!callBudgetTracker.tryAcquirePermit()) {
            log.warn("Open-Meteo AQ API call rejected: rate limit or daily budget exceeded. Coordinates: lat={}, lon={}",
                    latitude, longitude);
            return Optional.empty();
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                log.info("Fetching Open-Meteo AQ telemetry for lat={}, lon={}, pastDays={}, forecastDays={} (attempt {})",
                        latitude, longitude, pastDays, forecastDays, attempt);

                String responseBody = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("latitude", latitude)
                                .queryParam("longitude", longitude)
                                .queryParam("hourly", "pm10,pm2_5,nitrogen_dioxide,sulphur_dioxide,ozone,carbon_monoxide")
                                .queryParam("past_days", pastDays)
                                .queryParam("forecast_days", forecastDays)
                                .queryParam("timezone", "Asia/Kolkata")
                                .build())
                        .retrieve()
                        .body(String.class);

                if (responseBody == null || responseBody.isBlank()) {
                    log.warn("Empty response received from Open-Meteo AQ API for lat={}, lon={}", latitude, longitude);
                    return Optional.empty();
                }

                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode hourlyNode = rootNode.get("hourly");
                if (hourlyNode == null) {
                    log.warn("No 'hourly' node present in Open-Meteo AQ response for lat={}, lon={}", latitude, longitude);
                    return Optional.empty();
                }

                JsonNode timeNode = hourlyNode.get("time");
                JsonNode pm10Node = hourlyNode.get("pm10");
                JsonNode pm25Node = hourlyNode.get("pm2_5");
                JsonNode no2Node = hourlyNode.get("nitrogen_dioxide");
                JsonNode so2Node = hourlyNode.get("sulphur_dioxide");
                JsonNode ozoneNode = hourlyNode.get("ozone");
                JsonNode coNode = hourlyNode.get("carbon_monoxide");

                if (timeNode == null || !timeNode.isArray()) {
                    log.warn("Missing 'time' array in Open-Meteo AQ response for lat={}, lon={}", latitude, longitude);
                    return Optional.empty();
                }

                List<RollingAverageCalculator.RawHourlyAq> rawHourly = new ArrayList<>(timeNode.size());
                for (int i = 0; i < timeNode.size(); i++) {
                    String time = timeNode.get(i).asText();
                    Double pm10 = getNodeDouble(pm10Node, i);
                    Double pm25 = getNodeDouble(pm25Node, i);
                    Double no2 = getNodeDouble(no2Node, i);
                    Double so2 = getNodeDouble(so2Node, i);
                    Double o3 = getNodeDouble(ozoneNode, i);
                    Double coUgM3 = getNodeDouble(coNode, i);

                    rawHourly.add(new RollingAverageCalculator.RawHourlyAq(
                            time, pm10, pm25, no2, so2, o3, coUgM3
                    ));
                }

                log.info("Successfully fetched and parsed {} hours of AQ telemetry for lat={}, lon={}",
                        rawHourly.size(), latitude, longitude);

                return Optional.of(new AqForecastResult(
                        Instant.now(),
                        latitude,
                        longitude,
                        rawHourly
                ));

            } catch (ResourceAccessException ex) {
                if (attempt == 1) {
                    log.warn("Network I/O error fetching AQ telemetry for lat={}, lon={}: {}. Retrying after 250ms...",
                            latitude, longitude, ex.getMessage());
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Optional.empty();
                    }
                    continue;
                }
                log.warn("Network I/O error fetching AQ telemetry on retry for lat={}, lon={}: {}",
                        latitude, longitude, ex.getMessage());
                return Optional.empty();
            } catch (Exception ex) {
                log.warn("Failed to fetch live Open-Meteo AQ telemetry for lat={}, lon={}: {}",
                        latitude, longitude, ex.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Double getNodeDouble(JsonNode node, int index) {
        if (node != null && node.isArray() && index < node.size()) {
            JsonNode val = node.get(index);
            if (val != null && !val.isNull()) {
                return val.asDouble();
            }
        }
        return null;
    }

    public CallBudgetTracker getCallBudgetTracker() {
        return callBudgetTracker;
    }

    public record AqForecastResult(
            Instant fetchedAt,
            double latitude,
            double longitude,
            List<RollingAverageCalculator.RawHourlyAq> rawHourly
    ) {}
}

