package com.kestrel.floodingest.infra.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.floodingest.domain.ExceedanceCalculator;
import com.kestrel.floodingest.infra.budget.CallBudgetTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CallBudgetTracker callBudgetTracker;

    public OpenMeteoClient(
            @Qualifier("openMeteoRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            CallBudgetTracker callBudgetTracker) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.callBudgetTracker = callBudgetTracker;
    }

    public Optional<OpenMeteoForecastResult> fetchEnsembleForecast(
            double latitude,
            double longitude,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr) {
        return fetchEnsembleForecast(latitude, longitude, threshold2yr, threshold5yr, threshold20yr, 10);
    }

    public Optional<OpenMeteoForecastResult> fetchEnsembleForecast(
            double latitude,
            double longitude,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            int forecastDays) {

        // Check and atomically consume budget quota before invoking external API
        if (!callBudgetTracker.tryAcquirePermit()) {
            log.warn("Open-Meteo API call rejected: rate limit or daily budget exceeded (< 500 req/min or < 8000 req/day). Coordinates: lat={}, lon={}",
                    latitude, longitude);
            return Optional.empty();
        }

        try {
            log.info("Fetching Open-Meteo flood ensemble forecast for lat={}, lon={}, forecastDays={}",
                    latitude, longitude, forecastDays);

            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "river_discharge,river_discharge_median,river_discharge_max,river_discharge_min,river_discharge_p25,river_discharge_p75")
                            .queryParam("ensemble", "true")
                            .queryParam("forecast_days", forecastDays)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Empty response received from Open-Meteo flood API for lat={}, lon={}", latitude, longitude);
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode dailyNode = rootNode.get("daily");
            if (dailyNode == null) {
                log.warn("No 'daily' node present in Open-Meteo response for lat={}, lon={}", latitude, longitude);
                return Optional.empty();
            }

            // Parse dates
            JsonNode timeNode = dailyNode.get("time");
            List<LocalDate> dates = new ArrayList<>();
            if (timeNode != null && timeNode.isArray()) {
                timeNode.forEach(val -> dates.add(LocalDate.parse(val.asText())));
            }

            // Dynamic ensemble member parsing (50 vs 51 GloFAS members)
            Map<String, List<Double>> members = new HashMap<>();
            dailyNode.fieldNames().forEachRemaining(field -> {
                if (field.startsWith("river_discharge_member")) {
                    JsonNode memberArray = dailyNode.get(field);
                    if (memberArray != null && memberArray.isArray()) {
                        List<Double> discharge = new ArrayList<>();
                        memberArray.forEach(val -> {
                            if (val != null && !val.isNull()) {
                                discharge.add(val.asDouble());
                            } else {
                                discharge.add(0.0);
                            }
                        });
                        members.put(field, discharge);
                    }
                }
            });

            // Fallback to deterministic river_discharge if no member keys were returned
            if (members.isEmpty() && dailyNode.has("river_discharge")) {
                JsonNode deterministicNode = dailyNode.get("river_discharge");
                if (deterministicNode != null && deterministicNode.isArray()) {
                    List<Double> discharge = new ArrayList<>();
                    deterministicNode.forEach(val -> {
                        if (val != null && !val.isNull()) {
                            discharge.add(val.asDouble());
                        } else {
                            discharge.add(0.0);
                        }
                    });
                    members.put("river_discharge_deterministic", discharge);
                }
            }

            int memberCount = members.size();
            log.info("Parsed {} GloFAS ensemble members across {} forecast days for lat={}, lon={}",
                    memberCount, dates.size(), latitude, longitude);

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
            log.warn("Failed to fetch live Open-Meteo forecast for lat={}, lon={}: {}",
                    latitude, longitude, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<HistoricalDischargeResult> fetchHistoricalDailyDischarge(
            double latitude,
            double longitude) {
        LocalDate startDate = LocalDate.of(1984, 1, 1);
        LocalDate endDate = LocalDate.now().minusDays(1);
        return fetchHistoricalDailyDischarge(latitude, longitude, startDate, endDate);
    }

    public Optional<HistoricalDischargeResult> fetchHistoricalDailyDischarge(
            double latitude,
            double longitude,
            LocalDate startDate,
            LocalDate endDate) {

        if (!callBudgetTracker.tryAcquirePermit()) {
            log.warn("Open-Meteo API call rejected: rate limit or daily budget exceeded. Coordinates: lat={}, lon={}",
                    latitude, longitude);
            return Optional.empty();
        }

        try {
            log.info("Fetching Open-Meteo historical flood discharge for lat={}, lon={}, start={}, end={}",
                    latitude, longitude, startDate, endDate);

            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "river_discharge")
                            .queryParam("start_date", startDate.toString())
                            .queryParam("end_date", endDate.toString())
                            .build())
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Empty response received from Open-Meteo flood archive for lat={}, lon={}", latitude, longitude);
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode dailyNode = rootNode.get("daily");
            if (dailyNode == null) {
                log.warn("No 'daily' node present in Open-Meteo archive response for lat={}, lon={}", latitude, longitude);
                return Optional.empty();
            }

            JsonNode timeNode = dailyNode.get("time");
            JsonNode dischargeNode = dailyNode.get("river_discharge");
            if (timeNode == null || dischargeNode == null || !timeNode.isArray() || !dischargeNode.isArray()) {
                log.warn("Missing time or river_discharge array in Open-Meteo archive for lat={}, lon={}", latitude, longitude);
                return Optional.empty();
            }

            List<LocalDate> dates = new ArrayList<>(timeNode.size());
            List<Double> discharges = new ArrayList<>(dischargeNode.size());
            Map<Integer, Double> annualMaxima = new java.util.TreeMap<>();

            for (int i = 0; i < timeNode.size(); i++) {
                JsonNode tNode = timeNode.get(i);
                JsonNode dNode = dischargeNode.get(i);
                LocalDate d = LocalDate.parse(tNode.asText());
                dates.add(d);

                Double val = (dNode != null && !dNode.isNull()) ? dNode.asDouble() : 0.0;
                discharges.add(val);

                if (val > 0.0) {
                    annualMaxima.merge(d.getYear(), val, Math::max);
                }
            }

            log.info("Extracted {} daily records and {} annual maxima for lat={}, lon={}",
                    dates.size(), annualMaxima.size(), latitude, longitude);

            return Optional.of(new HistoricalDischargeResult(
                    latitude,
                    longitude,
                    startDate,
                    endDate,
                    dates,
                    discharges,
                    annualMaxima
            ));

        } catch (Exception ex) {
            log.warn("Failed to fetch historical discharge from Open-Meteo for lat={}, lon={}: {}",
                    latitude, longitude, ex.getMessage());
            return Optional.empty();
        }
    }

    public CallBudgetTracker.BudgetStatus getBudgetStatus() {
        return callBudgetTracker.getBudgetStatus();
    }

    public CallBudgetTracker getCallBudgetTracker() {
        return callBudgetTracker;
    }

    public record HistoricalDischargeResult(
            double latitude,
            double longitude,
            LocalDate startDate,
            LocalDate endDate,
            List<LocalDate> dates,
            List<Double> dailyDischarges,
            Map<Integer, Double> annualMaxima
    ) {}

    public record OpenMeteoForecastResult(
            Instant dataAsOf,
            int memberCount,
            List<LocalDate> dates,
            Map<String, List<Double>> memberDischarges,
            List<ForecastDayStats> dayStats,
            boolean isStale
    ) {}
}
