package com.kestrel.floodingest.infra.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.floodingest.domain.ExceedanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoClient(
            @Qualifier("openMeteoRestClient") RestClient restClient,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public Optional<OpenMeteoForecastResult> fetchEnsembleForecast(
            double latitude,
            double longitude,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr) {
        try {
            log.info("Fetching Open-Meteo flood ensemble forecast for lat={}, lon={}", latitude, longitude);

            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "river_discharge,river_discharge_median,river_discharge_max,river_discharge_min,river_discharge_p25,river_discharge_p75")
                            .queryParam("ensemble", "true")
                            .queryParam("forecast_days", 10)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Empty response received from Open-Meteo flood API");
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode dailyNode = rootNode.get("daily");
            if (dailyNode == null) {
                log.warn("No 'daily' node present in Open-Meteo response");
                return Optional.empty();
            }

            // Parse dates
            JsonNode timeNode = dailyNode.get("time");
            List<LocalDate> dates = new ArrayList<>();
            if (timeNode != null && timeNode.isArray()) {
                timeNode.forEach(val -> dates.add(LocalDate.parse(val.asText())));
            }

            // Dynamic ensemble member parsing (50 vs 51 members)
            Map<String, List<Double>> members = new HashMap<>();
            dailyNode.fieldNames().forEachRemaining(field -> {
                if (field.startsWith("river_discharge_member")) {
                    List<Double> discharge = new ArrayList<>();
                    dailyNode.get(field).forEach(val -> discharge.add(val.asDouble()));
                    members.put(field, discharge);
                }
            });

            // Fallback to deterministic river_discharge if no member keys were returned
            if (members.isEmpty() && dailyNode.has("river_discharge")) {
                List<Double> discharge = new ArrayList<>();
                dailyNode.get("river_discharge").forEach(val -> discharge.add(val.asDouble()));
                members.put("river_discharge_deterministic", discharge);
            }

            int memberCount = members.size();
            log.info("Parsed {} ensemble members across {} forecast days", memberCount, dates.size());

            List<ForecastDayStats> dayStats = ExceedanceCalculator.calculateForecast(
                    dates,
                    members,
                    threshold2yr,
                    threshold5yr,
                    threshold20yr
            );

            return Optional.of(new OpenMeteoForecastResult(
                    Instant.now(),
                    memberCount,
                    dates,
                    members,
                    dayStats,
                    false
            ));

        } catch (Exception ex) {
            log.warn("Failed to fetch live Open-Meteo forecast: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public record OpenMeteoForecastResult(
            Instant dataAsOf,
            int memberCount,
            List<LocalDate> dates,
            Map<String, List<Double>> memberDischarges,
            List<ForecastDayStats> dayStats,
            boolean isStale
    ) {}
}
