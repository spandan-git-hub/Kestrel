package com.kestrel.aqingest.infra.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.aqingest.domain.RollingAverageCalculator;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoAqClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private InMemoryCallBudgetTracker callBudgetTracker;
    private OpenMeteoAqClient client;
    private final ObjectMapper objectMapper = EventObjectMapperFactory.createObjectMapper();

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("https://air-quality-api.open-meteo.com/v1/air-quality");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        callBudgetTracker = new InMemoryCallBudgetTracker(500, 8000);
        client = new OpenMeteoAqClient(restClient, objectMapper, callBudgetTracker);
    }

    @Test
    @DisplayName("Successfully queries hourly Air Quality telemetry for CAMS pollutants")
    void successfullyParsesHourlyAqTelemetry() {
        String jsonResponse = """
                {
                  "latitude": 28.66,
                  "longitude": 77.24,
                  "timezone": "Asia/Kolkata",
                  "hourly": {
                    "time": ["2026-10-31T00:00", "2026-10-31T01:00", "2026-10-31T02:00"],
                    "pm10": [120.5, 130.0, 125.0],
                    "pm2_5": [75.0, 80.5, 78.0],
                    "nitrogen_dioxide": [45.2, 48.0, 42.1],
                    "sulphur_dioxide": [15.0, 14.5, 16.2],
                    "ozone": [35.0, 30.0, 28.5],
                    "carbon_monoxide": [1200.0, 1350.0, 1100.0]
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("latitude=28.66")))
                .andExpect(requestTo(containsString("longitude=77.24")))
                .andExpect(requestTo(containsString("hourly=pm10,pm2_5,nitrogen_dioxide,sulphur_dioxide,ozone,carbon_monoxide")))
                .andExpect(requestTo(containsString("past_days=2")))
                .andExpect(requestTo(containsString("forecast_days=3")))
                .andExpect(requestTo(containsString("timezone=Asia/Kolkata")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<OpenMeteoAqClient.AqForecastResult> resultOpt =
                client.fetchHourlyAq(28.66, 77.24, 2, 3);

        mockServer.verify();
        assertThat(resultOpt).isPresent();

        OpenMeteoAqClient.AqForecastResult result = resultOpt.get();
        assertThat(result.rawHourly()).hasSize(3);

        RollingAverageCalculator.RawHourlyAq h0 = result.rawHourly().get(0);
        assertThat(h0.time()).isEqualTo("2026-10-31T00:00");
        assertThat(h0.pm10()).isEqualTo(120.5);
        assertThat(h0.pm25()).isEqualTo(75.0);
        assertThat(h0.no2()).isEqualTo(45.2);
        assertThat(h0.so2()).isEqualTo(15.0);
        assertThat(h0.o3()).isEqualTo(35.0);
        assertThat(h0.coUgM3()).isEqualTo(1200.0);
        assertThat(h0.coMgM3()).isEqualTo(1.2);

        // Call budget permit consumed
        assertThat(callBudgetTracker.getBudgetStatus().currentMinuteCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rejects API call when minute rate limit quota is reached")
    void rejectsCallWhenMinuteRateLimitReached() {
        String minuteKey = DynamoDbCallBudgetTracker.getMinuteKey(Instant.now().getEpochSecond());
        callBudgetTracker.setCallCount(minuteKey, 500);

        Optional<OpenMeteoAqClient.AqForecastResult> resultOpt =
                client.fetchHourlyAq(28.66, 77.24);

        assertThat(resultOpt).isEmpty();
        mockServer.verify();
    }
}

