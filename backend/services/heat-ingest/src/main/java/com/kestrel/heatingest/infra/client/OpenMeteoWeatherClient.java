package com.kestrel.heatingest.infra.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.core.budget.CallBudgetTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OpenMeteoWeatherClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoWeatherClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CallBudgetTracker callBudgetTracker;

    public OpenMeteoWeatherClient(
            @Qualifier("openMeteoWeatherRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            CallBudgetTracker callBudgetTracker) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.callBudgetTracker = callBudgetTracker;
    }

    public Optional<WeatherForecastResult> fetch7DayForecast(double latitude, double longitude) {
        return fetchForecast(latitude, longitude, 7);
    }

    public Optional<WeatherForecastResult> fetchForecast(double latitude, double longitude, int forecastDays) {
        // Enforce DynamoDB atomic call budget (< 500 req/min, < 8000 req/day) shared with flood ingest
        if (!callBudgetTracker.tryAcquirePermit()) {
            log.warn("Open-Meteo Weather API call rejected: rate limit or daily budget exceeded. Coordinates: lat={}, lon={}",
                    latitude, longitude);
            return Optional.empty();
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                log.info("Fetching Open-Meteo weather forecast for lat={}, lon={}, forecastDays={} (attempt {})",
                        latitude, longitude, forecastDays, attempt);

                String responseBody = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("latitude", latitude)
                                .queryParam("longitude", longitude)
                                .queryParam("daily", "temperature_2m_max,apparent_temperature_max,relative_humidity_2m_max")
                                .queryParam("timezone", "Asia/Kolkata")
                                .queryParam("forecast_days", forecastDays)
                                .build())
                        .retrieve()
                        .body(String.class);

                if (responseBody == null || responseBody.isBlank()) {
                    log.warn("Empty response received from Open-Meteo weather API for lat={}, lon={}", latitude, longitude);
                    return Optional.empty();
                }

                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode dailyNode = rootNode.get("daily");
                if (dailyNode == null) {
                    log.warn("No 'daily' node present in Open-Meteo weather response for lat={}, lon={}", latitude, longitude);
                    return Optional.empty();
                }

                JsonNode timeNode = dailyNode.get("time");
                JsonNode tmaxNode = dailyNode.get("temperature_2m_max");
                JsonNode appMaxNode = dailyNode.get("apparent_temperature_max");
                JsonNode humNode = dailyNode.get("relative_humidity_2m_max");

                if (timeNode == null || !timeNode.isArray() || tmaxNode == null || !tmaxNode.isArray()) {
                    log.warn("Incomplete daily arrays in Open-Meteo weather response for lat={}, lon={}", latitude, longitude);
                    return Optional.empty();
                }

                List<DailyWeatherRecord> dailyRecords = new ArrayList<>(timeNode.size());
                for (int i = 0; i < timeNode.size(); i++) {
                    LocalDate date = LocalDate.parse(timeNode.get(i).asText());
                    double tmax = (tmaxNode.get(i) != null && !tmaxNode.get(i).isNull()) ? tmaxNode.get(i).asDouble() : 0.0;
                    double apparentMax = (appMaxNode != null && appMaxNode.get(i) != null && !appMaxNode.get(i).isNull())
                            ? appMaxNode.get(i).asDouble() : tmax;
                    double humidityMax = (humNode != null && humNode.get(i) != null && !humNode.get(i).isNull())
                            ? humNode.get(i).asDouble() : 50.0;

                    dailyRecords.add(new DailyWeatherRecord(date, tmax, apparentMax, humidityMax));
                }

                log.info("Successfully parsed {} forecast days of weather telemetry for lat={}, lon={}",
                        dailyRecords.size(), latitude, longitude);

                return Optional.of(new WeatherForecastResult(
                        Instant.now(),
                        latitude,
                        longitude,
                        dailyRecords
                ));

            } catch (ResourceAccessException ex) {
                if (attempt == 1) {
                    log.warn("Network I/O error fetching weather forecast for lat={}, lon={}: {}. Retrying after 250ms...",
                            latitude, longitude, ex.getMessage());
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Optional.empty();
                    }
                    continue;
                }
                log.warn("Network I/O error fetching weather forecast on retry for lat={}, lon={}: {}",
                        latitude, longitude, ex.getMessage());
                return Optional.empty();
            } catch (Exception ex) {
                log.warn("Failed to fetch live Open-Meteo weather forecast for lat={}, lon={}: {}",
                        latitude, longitude, ex.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public CallBudgetTracker getCallBudgetTracker() {
        return callBudgetTracker;
    }

    public record DailyWeatherRecord(
            LocalDate date,
            double temperature2mMax,
            double apparentTemperatureMax,
            double relativeHumidity2mMax
    ) {}

    public record WeatherForecastResult(
            Instant fetchedAt,
            double latitude,
            double longitude,
            List<DailyWeatherRecord> dailyRecords
    ) {}
}

