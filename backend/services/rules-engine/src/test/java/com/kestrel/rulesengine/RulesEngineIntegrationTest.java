package com.kestrel.rulesengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.rulesengine.infra.repository.AlertHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class RulesEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertHistoryRepository alertHistoryRepository;

    @Test
    @DisplayName("GET /api/v1/rules/status returns initial 4 seeded pilot locations")
    void getStatusReturnsSeededLocations() throws Exception {
        mockMvc.perform(get("/api/v1/rules/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].locationId", notNullValue()))
                .andExpect(jsonPath("$[0].hazard", is("FLOOD")))
                .andExpect(jsonPath("$[0].provenance", is("MODEL_ONLY")))
                .andExpect(jsonPath("$[0].isStale", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/rules/forecast/{locationId} returns thresholds and forecast days")
    void getForecastReturnsThresholdsAndDays() throws Exception {
        mockMvc.perform(get("/api/v1/rules/forecast/mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId", is("mumbai-kurla")))
                .andExpect(jsonPath("$.thresholds.rp2", is(45.0)))
                .andExpect(jsonPath("$.thresholds.rp5", is(85.0)))
                .andExpect(jsonPath("$.thresholds.rp20", is(140.0)))
                .andExpect(jsonPath("$.days", hasSize(greaterThanOrEqualTo(10))));
    }

    @Test
    @DisplayName("POST /api/v1/rules/events/flood-signal processes exceedance, transitions state and emits AlertEvent")
    void processFloodSignalTransitionsState() throws Exception {
        LocalDate today = LocalDate.now();
        List<ForecastDayStats> days = new ArrayList<>();
        // Day 1 spike exceeding 5yr threshold at 68% (WARNING)
        days.add(new ForecastDayStats(today, 15, 20, 30, 40, 50, 0.1, 0.0, 0.0));
        days.add(new ForecastDayStats(today.plusDays(1), 70, 85, 95, 110, 130, 0.95, 0.68, 0.15));
        for (int i = 2; i < 10; i++) {
            days.add(new ForecastDayStats(today.plusDays(i), 15, 20, 30, 40, 50, 0.1, 0.0, 0.0));
        }

        FloodSignalPayload payload = new FloodSignalPayload(
                "patna-ward12",
                Instant.now(),
                true, // simulated -> triggers immediate persistence
                51,
                days
        );

        EventEnvelope<FloodSignalPayload> envelope = EventEnvelope.of(
                "FloodSignal",
                "flood-ingest",
                "test-corr-id",
                payload
        );

        mockMvc.perform(post("/api/v1/rules/events/flood-signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));

        // Verify alert history transition was recorded
        assertThat(alertHistoryRepository.findByLocationIdOrderByTransitionedAtDesc("patna-ward12"))
                .isNotEmpty()
                .anySatisfy(hist -> {
                    assertThat(hist.getToSeverity().name()).isEqualTo("WARNING");
                    assertThat(hist.getPeakExceedanceShare()).isEqualTo(0.68);
                });

        // Verify status reflects WARNING severity
        mockMvc.perform(get("/api/v1/rules/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.locationId == 'patna-ward12')].severity").value("WARNING"))
                .andExpect(jsonPath("$[?(@.locationId == 'patna-ward12')].activeAlertId", notNullValue()))
                .andExpect(jsonPath("$[?(@.locationId == 'patna-ward12')].peakExceedanceShare").value(0.68));
    }
}
