package com.kestrel.floodingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.LocationUpsertedPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
    @DisplayName("POST /api/v1/flood/sync-live/{locationId} syncs real live forecast for location")
    void shouldSyncLiveLocation() throws Exception {
        mockMvc.perform(post("/api/v1/flood/sync-live/mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.memberCount").isNumber());
    }

    @Test
    @DisplayName("POST /api/v1/flood/sync-live triggers full sync across all active stations")
    void shouldSyncLiveAll() throws Exception {
        mockMvc.perform(post("/api/v1/flood/sync-live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedLocations").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/flood/forecast/{locationId} returns forecast and thresholds")
    void shouldReturnForecastForLocation() throws Exception {
        // Sync live first
        mockMvc.perform(post("/api/v1/flood/sync-live/mumbai-kurla"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/flood/forecast/mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.thresholds.rp2").value(45.0))
                .andExpect(jsonPath("$.thresholds.rp5").value(85.0))
                .andExpect(jsonPath("$.thresholds.rp20").value(140.0));
    }

    @Test
    @DisplayName("GET /api/v1/flood/locations returns all seeded Indian river monitoring stations")
    void shouldReturnAllLocations() throws Exception {
        mockMvc.perform(get("/api/v1/flood/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(45))));
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
    }

    @Test
    @DisplayName("POST /api/v1/flood/thresholds/fit/{locationId} fits thresholds and returns result")
    void shouldFitThresholdsViaEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/flood/thresholds/fit/mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.threshold2yr").isNumber())
                .andExpect(jsonPath("$.threshold5yr").isNumber())
                .andExpect(jsonPath("$.threshold20yr").isNumber());

        mockMvc.perform(get("/api/v1/flood/thresholds"))
                .andExpect(status().isOk());
    }
}
