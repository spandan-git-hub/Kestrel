package com.kestrel.heatingest.infra.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.core.budget.DynamoDbCallBudgetTracker;
import com.kestrel.common.core.budget.InMemoryCallBudgetTracker;
import com.kestrel.common.events.EventObjectMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoWeatherClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private InMemoryCallBudgetTracker callBudgetTracker;
    private OpenMeteoWeatherClient client;
    private final ObjectMapper objectMapper = EventObjectMapperFactory.createObjectMapper();

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("https://api.open-meteo.com/v1/forecast");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        callBudgetTracker = new InMemoryCallBudgetTracker(500, 8000);
        client = new OpenMeteoWeatherClient(restClient, objectMapper, callBudgetTracker);
    }

    @Test
    @DisplayName("Successfully queries 7-day weather forecast with max temperature, apparent temperature, and humidity")
    void successfullyParses7DayForecast() {
        String jsonResponse = """
                {
                  "latitude": 28.66,
                  "longitude": 77.24,
                  "timezone": "Asia/Kolkata",
                  "daily": {
                    "time": ["2026-10-31", "2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06"],
                    "temperature_2m_max": [32.4, 33.1, 34.0, 33.8, 32.9, 31.5, 31.0],
                    "apparent_temperature_max": [34.1, 35.0, 36.2, 35.8, 34.5, 33.0, 32.2],
                    "relative_humidity_2m_max": [55.0, 52.0, 48.0, 50.0, 54.0, 58.0, 60.0]
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("latitude=28.66")))
                .andExpect(requestTo(containsString("longitude=77.24")))
                .andExpect(requestTo(containsString("daily=temperature_2m_max,apparent_temperature_max,relative_humidity_2m_max")))
                .andExpect(requestTo(containsString("timezone=Asia/Kolkata")))
                .andExpect(requestTo(containsString("forecast_days=7")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<OpenMeteoWeatherClient.WeatherForecastResult> resultOpt =
                client.fetch7DayForecast(28.66, 77.24);

        mockServer.verify();
        assertThat(resultOpt).isPresent();

        OpenMeteoWeatherClient.WeatherForecastResult result = resultOpt.get();
        assertThat(result.dailyRecords()).hasSize(7);

        OpenMeteoWeatherClient.DailyWeatherRecord day1 = result.dailyRecords().get(0);
        assertThat(day1.date()).isEqualTo(LocalDate.parse("2026-10-31"));
        assertThat(day1.temperature2mMax()).isEqualTo(32.4);
        assertThat(day1.apparentTemperatureMax()).isEqualTo(34.1);
        assertThat(day1.relativeHumidity2mMax()).isEqualTo(55.0);

        // Budget was consumed
        assertThat(callBudgetTracker.getBudgetStatus().currentMinuteCount()).isEqualTo(1);
        assertThat(callBudgetTracker.getBudgetStatus().currentDayCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rejects API call when DynamoDB minute rate limit quota is reached")
    void rejectsCallWhenMinuteRateLimitReached() {
        String minuteKey = DynamoDbCallBudgetTracker.getMinuteKey(Instant.now().getEpochSecond());
        callBudgetTracker.setCallCount(minuteKey, 500); // Max minute limit is 500

        Optional<OpenMeteoWeatherClient.WeatherForecastResult> resultOpt =
                client.fetch7DayForecast(28.66, 77.24);

        // Call should be rejected before any network request
        assertThat(resultOpt).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Rejects API call when daily budget quota is reached")
    void rejectsCallWhenDailyBudgetReached() {
        String dayKey = DynamoDbCallBudgetTracker.getDayKey();
        callBudgetTracker.setCallCount(dayKey, 8000); // Max day limit is 8000

        Optional<OpenMeteoWeatherClient.WeatherForecastResult> resultOpt =
                client.fetch7DayForecast(28.66, 77.24);

        assertThat(resultOpt).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Returns empty optional on server error after retry")
    void returnsEmptyOnServerError() {
        mockServer.expect(requestTo(containsString("latitude=28.66")))
                .andRespond(withServerError());

        Optional<OpenMeteoWeatherClient.WeatherForecastResult> resultOpt =
                client.fetch7DayForecast(28.66, 77.24);

        assertThat(resultOpt).isEmpty();
    }
}
