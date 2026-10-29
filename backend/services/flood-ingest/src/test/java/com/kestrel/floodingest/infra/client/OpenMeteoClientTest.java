package com.kestrel.floodingest.infra.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventObjectMapperFactory;
import com.kestrel.floodingest.infra.budget.DynamoDbCallBudgetTracker;
import com.kestrel.floodingest.infra.budget.InMemoryCallBudgetTracker;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private InMemoryCallBudgetTracker callBudgetTracker;
    private OpenMeteoClient client;
    private final ObjectMapper objectMapper = EventObjectMapperFactory.createObjectMapper();

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("https://flood-api.open-meteo.com/v1/flood");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        callBudgetTracker = new InMemoryCallBudgetTracker(500, 8000);
        client = new OpenMeteoClient(restClient, objectMapper, callBudgetTracker);
    }

    @Test
    @DisplayName("Parses 50 GloFAS ensemble members and computes exceedances")
    void shouldParse50MembersSuccessfully() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
            .append("  \"latitude\": 19.0726,\n")
            .append("  \"longitude\": 72.8797,\n")
            .append("  \"daily\": {\n")
            .append("    \"time\": [\"2026-10-22\", \"2026-10-23\"],\n");

        for (int i = 1; i <= 50; i++) {
            String memberKey = String.format("river_discharge_member%02d", i);
            double val1 = 20.0 + i;
            double val2 = 50.0 + (i * 2);
            json.append(String.format("    \"%s\": [%.1f, %.1f]%s\n", memberKey, val1, val2, i == 50 ? "" : ","));
        }
        json.append("  }\n}");

        mockServer.expect(requestTo(containsString("latitude=19.0726")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json.toString(), MediaType.APPLICATION_JSON));

        Optional<OpenMeteoClient.OpenMeteoForecastResult> result = client.fetchEnsembleForecast(
                19.0726, 72.8797, 45.0, 85.0, 140.0);

        assertThat(result).isPresent();
        assertThat(result.get().memberCount()).isEqualTo(50);
        assertThat(result.get().dates()).hasSize(2);
        assertThat(result.get().dayStats()).hasSize(2);
        assertThat(callBudgetTracker.getBudgetStatus().currentMinuteCount()).isEqualTo(1);
        assertThat(callBudgetTracker.getBudgetStatus().currentDayCount()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    @DisplayName("Parses 51 GloFAS ensemble members dynamically (51-member ECMWF cycle)")
    void shouldParse51MembersSuccessfully() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
            .append("  \"latitude\": 25.6127,\n")
            .append("  \"longitude\": 85.1589,\n")
            .append("  \"daily\": {\n")
            .append("    \"time\": [\"2026-10-29\", \"2026-10-30\"],\n");

        for (int i = 0; i <= 50; i++) {
            String memberKey = String.format("river_discharge_member%02d", i);
            double val1 = 30000.0 + (i * 200);
            double val2 = 45000.0 + (i * 300);
            json.append(String.format("    \"%s\": [%.1f, %.1f]%s\n", memberKey, val1, val2, i == 50 ? "" : ","));
        }
        json.append("  }\n}");

        mockServer.expect(requestTo(containsString("latitude=25.6127")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json.toString(), MediaType.APPLICATION_JSON));

        Optional<OpenMeteoClient.OpenMeteoForecastResult> result = client.fetchEnsembleForecast(
                25.6127, 85.1589, 38000.0, 48000.0, 62000.0);

        assertThat(result).isPresent();
        assertThat(result.get().memberCount()).isEqualTo(51);
        assertThat(result.get().dates()).hasSize(2);
        assertThat(result.get().dayStats()).hasSize(2);
        mockServer.verify();
    }

    @Test
    @DisplayName("Rejects Open-Meteo call when minute budget limit is exceeded")
    void shouldRejectCallWhenMinuteBudgetExceeded() {
        // Pre-fill minute limit to max (500)
        String minuteKey = DynamoDbCallBudgetTracker.getMinuteKey(Instant.now().getEpochSecond());
        callBudgetTracker.setCallCount(minuteKey, 500);

        Optional<OpenMeteoClient.OpenMeteoForecastResult> result = client.fetchEnsembleForecast(
                19.0726, 72.8797, 45.0, 85.0, 140.0);

        assertThat(result).isEmpty();
        // Mock server should not have received any HTTP requests
        mockServer.verify();
    }

    @Test
    @DisplayName("Rejects Open-Meteo call when daily budget limit is exceeded")
    void shouldRejectCallWhenDayBudgetExceeded() {
        // Pre-fill day limit to max (8000)
        String dayKey = DynamoDbCallBudgetTracker.getDayKey();
        callBudgetTracker.setCallCount(dayKey, 8000);

        Optional<OpenMeteoClient.OpenMeteoForecastResult> result = client.fetchEnsembleForecast(
                19.0726, 72.8797, 45.0, 85.0, 140.0);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Falls back to deterministic discharge when no member keys are returned")
    void shouldFallbackToDeterministicDischargeWhenNoMembers() {
        String json = """
                {
                  "latitude": 19.0726,
                  "longitude": 72.8797,
                  "daily": {
                    "time": ["2026-10-29"],
                    "river_discharge": [55.5]
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("latitude=19.0726")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<OpenMeteoClient.OpenMeteoForecastResult> result = client.fetchEnsembleForecast(
                19.0726, 72.8797, 45.0, 85.0, 140.0);

        assertThat(result).isPresent();
        assertThat(result.get().memberCount()).isEqualTo(1);
        assertThat(result.get().memberDischarges()).containsKey("river_discharge_deterministic");
        mockServer.verify();
    }

    @Test
    @DisplayName("Handles Open-Meteo 5xx/429 gracefully with empty result for fallback")
    void shouldHandleApiFailureGracefully() {
        mockServer.expect(requestTo(containsString("latitude=19.0726")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Optional<OpenMeteoClient.OpenMeteoForecastResult> result = client.fetchEnsembleForecast(
                19.0726, 72.8797, 45.0, 85.0, 140.0);

        assertThat(result).isEmpty();
        mockServer.verify();
    }
}
