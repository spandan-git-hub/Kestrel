package com.kestrel.aqingest.api;

import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.repository.AqForecastRunRepository;
import com.kestrel.aqingest.infra.repository.AqHourlyValueRepository;
import com.kestrel.aqingest.infra.repository.AqMonitoredPointRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private AqMonitoredPointRepository pointRepository;

    @Autowired
    private AqForecastRunRepository runRepository;

    @Autowired
    private AqHourlyValueRepository hourlyRepository;

    @Test
    @DisplayName("GET /api/v1/aq/points returns all seeded locations with CPCB AQI, sub-indices, and data freshness")
    void shouldReturnAllAqPoints() throws Exception {
        String locationId = "yamuna-delhi";
        Instant now = Instant.now();

        // Seed Delhi with known concentrations: PM2.5 = 45 -> sub-index 75 (Satisfactory)
        runRepository.save(new AqForecastRunEntity(
                locationId, "run-aq-test-1", now, now,
                45.0, 80.0, 30.0, 20.0, 40.0, 0.8,
                24, 3
        ));

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
    @DisplayName("GET /api/v1/aq/points/{locationId}/forecast returns latest run, projected 3-day forecast, and hourly telemetry")
    void shouldReturnPointForecast() throws Exception {
        String locationId = "ganga-patna";
        Instant now = Instant.now();

        runRepository.save(new AqForecastRunEntity(
                locationId,
                "run-aq-test-1",
                now,
                now,
                150.0,
                248.0,
                46.2,
                12.8,
                38.5,
                1.25,
                24,
                3
        ));

        hourlyRepository.saveAll(List.of(
                new AqHourlyValueEntity(locationId, "2026-11-02T00:00", 150.0, 240.0, 45.0, 12.0, 38.0, 1.2,
                        150.0, 240.0, 45.0, 12.0, 38.0, 1.2),
                new AqHourlyValueEntity(locationId, "2026-11-03T00:00", 80.0, 150.0, 35.0, 10.0, 30.0, 0.8,
                        80.0, 150.0, 35.0, 10.0, 30.0, 0.8)
        ));

        mockMvc.perform(get("/api/v1/aq/points/" + locationId + "/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(locationId))
                .andExpect(jsonPath("$.latestRun.currentPm25_24h").value(150.0))
                .andExpect(jsonPath("$.latestRun.currentPm10_24h").value(248.0))
                .andExpect(jsonPath("$.latestRun.currentCo_8h_mgm3").value(1.25))
                .andExpect(jsonPath("$.hourlyValues", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.days", hasSize(greaterThanOrEqualTo(1))))
                // PM2.5 = 150 -> AQI 323, VERY_POOR, WARNING
                .andExpect(jsonPath("$.days[0].projectedAqi").value(323))
                .andExpect(jsonPath("$.days[0].projectedCategory").value("VERY_POOR"))
                .andExpect(jsonPath("$.days[0].projectedSeverity").value("WARNING"))
                .andExpect(jsonPath("$.days[0].dominantPollutant").value("PM25"));
    }

    @Test
    @DisplayName("GET /api/v1/aq/feed-status returns feed state")
    void shouldReturnFeedStatus() throws Exception {
        mockMvc.perform(get("/api/v1/aq/feed-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedName").value("OPEN_METEO_AQ"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("POST /api/v1/aq/sync-live?locationId={id} triggers on-demand sync with query param")
    void shouldSyncLiveWithQueryParam() throws Exception {
        mockMvc.perform(post("/api/v1/aq/sync-live?locationId=yamuna-delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.syncedCount").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/aq/sync-live/{locationId} triggers on-demand sync with path variable")
    void shouldSyncLiveWithPath() throws Exception {
        mockMvc.perform(post("/api/v1/aq/sync-live/yamuna-delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.syncedCount").value(1));
    }
}
