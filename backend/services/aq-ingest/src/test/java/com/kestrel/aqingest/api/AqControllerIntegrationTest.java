package com.kestrel.aqingest.api;

import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.repository.AqForecastRunRepository;
import com.kestrel.aqingest.infra.repository.AqHourlyValueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "kestrel.persistence.type=inmemory"
)
@AutoConfigureMockMvc
class AqControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AqForecastRunRepository runRepository;

    @Autowired
    private AqHourlyValueRepository hourlyRepository;

    @Test
    @DisplayName("GET /api/v1/aq/points returns all seeded locations with data freshness")
    void shouldReturnAllAqPoints() throws Exception {
        mockMvc.perform(get("/api/v1/aq/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(45))))
                .andExpect(jsonPath("$[0].locationId").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].state").exists())
                .andExpect(jsonPath("$[0].latitude").isNumber())
                .andExpect(jsonPath("$[0].longitude").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/aq/points/{locationId}/forecast returns 404 when no forecast exists")
    void shouldReturnNotFoundForUnknownOrUnsyncedLocation() throws Exception {
        mockMvc.perform(get("/api/v1/aq/points/unknown-station/forecast"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/aq/points/{locationId}/forecast returns latest run and hourly telemetry")
    void shouldReturnPointForecast() throws Exception {
        String locationId = "yamuna-delhi";
        Instant now = Instant.now();

        runRepository.save(new AqForecastRunEntity(
                locationId,
                "run-aq-test-1",
                now,
                now,
                142.5,
                248.0,
                46.2,
                12.8,
                38.5,
                1.25,
                24,
                3
        ));

        hourlyRepository.saveAll(List.of(
                new AqHourlyValueEntity(locationId, "2026-10-31T00:00", 140.0, 240.0, 45.0, 12.0, 38.0, 1.2, 142.5, 248.0, 46.2, 12.8, 38.5, 1.25)
        ));

        mockMvc.perform(get("/api/v1/aq/points/" + locationId + "/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(locationId))
                .andExpect(jsonPath("$.latestRun.currentPm25_24h").value(142.5))
                .andExpect(jsonPath("$.latestRun.currentPm10_24h").value(248.0))
                .andExpect(jsonPath("$.latestRun.currentCo_8h_mgm3").value(1.25))
                .andExpect(jsonPath("$.hourlyValues", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/aq/feed-status returns feed state")
    void shouldReturnFeedStatus() throws Exception {
        mockMvc.perform(get("/api/v1/aq/feed-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedName").value("OPEN_METEO_AQ"))
                .andExpect(jsonPath("$.status").exists());
    }
}

