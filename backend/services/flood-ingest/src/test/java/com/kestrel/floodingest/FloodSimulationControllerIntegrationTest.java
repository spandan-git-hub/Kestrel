package com.kestrel.floodingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.SimulationScenario;
import com.kestrel.floodingest.api.dto.SimulationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class FloodSimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/flood/simulate injects WARNING_5YR scenario successfully")
    void shouldSimulateWarningScenario() throws Exception {
        SimulationRequest request = new SimulationRequest(
                SimulationScenario.WARNING_5YR,
                "mumbai-kurla"
        );

        mockMvc.perform(post("/api/v1/flood/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("WARNING_5YR"))
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.message").value(containsString("68% ensemble members")));
    }

    @Test
    @DisplayName("GET /api/v1/flood/forecast/{locationId} returns 10-day forecast and thresholds")
    void shouldReturnForecastForLocation() throws Exception {
        // Ensure data is populated by running simulation
        SimulationRequest request = new SimulationRequest(
                SimulationScenario.CALM,
                "mumbai-kurla"
        );
        mockMvc.perform(post("/api/v1/flood/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/flood/forecast/mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.thresholds.rp2").value(45.0))
                .andExpect(jsonPath("$.thresholds.rp5").value(85.0))
                .andExpect(jsonPath("$.thresholds.rp20").value(140.0))
                .andExpect(jsonPath("$.days", hasSize(10)));
    }

    @Test
    @DisplayName("POST /api/v1/flood/events/location-upserted syncs replica point")
    void shouldAcceptLocationUpsertedEvent() throws Exception {
        LocationUpsertedPayload payload = new LocationUpsertedPayload(
                "delhi-yamuna",
                "Yamuna Bridge, Delhi",
                "Delhi",
                28.6667,
                77.2167,
                "Yamuna River",
                205.33,
                206.50,
                208.00,
                Language.hi,
                true
        );
        EventEnvelope<LocationUpsertedPayload> envelope = EventEnvelope.of(
                "LocationUpserted",
                "subscriber-registry",
                "corr-123",
                payload
        );

        mockMvc.perform(post("/api/v1/flood/events/location-upserted")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk());

        // Verify we can now run simulation on newly synced location
        SimulationRequest simRequest = new SimulationRequest(
                SimulationScenario.CALM,
                "delhi-yamuna"
        );
        mockMvc.perform(post("/api/v1/flood/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(simRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value("delhi-yamuna"));
    }

    @Test
    @DisplayName("Validation failure returns RFC 7807 ProblemDetail format")
    void shouldReturnProblemDetailOnInvalidRequest() throws Exception {
        String invalidJson = "{\"scenario\": null, \"locationId\": \"\"}";

        mockMvc.perform(post("/api/v1/flood/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://kestrel.local/errors/validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }
}
