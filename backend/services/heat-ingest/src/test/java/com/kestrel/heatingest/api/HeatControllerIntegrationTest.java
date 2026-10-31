package com.kestrel.heatingest.api;

import com.kestrel.common.events.RegionType;
import com.kestrel.heatingest.infra.entity.HeatForecastDayEntity;
import com.kestrel.heatingest.infra.entity.HeatForecastRunEntity;
import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;
import com.kestrel.heatingest.infra.repository.HeatForecastDayRepository;
import com.kestrel.heatingest.infra.repository.HeatForecastRunRepository;
import com.kestrel.heatingest.infra.repository.HeatMonitoredPointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class HeatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HeatMonitoredPointRepository pointRepository;

    @Autowired
    private HeatForecastDayRepository dayRepository;

    @Autowired
    private HeatForecastRunRepository runRepository;

    @Test
    @DisplayName("GET /api/v1/heat/points returns all seeded locations with normals and data freshness")
    void shouldReturnAllHeatPoints() throws Exception {
        mockMvc.perform(get("/api/v1/heat/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(45))))
                .andExpect(jsonPath("$[0].locationId").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].state").exists())
                .andExpect(jsonPath("$[0].latitude").isNumber())
                .andExpect(jsonPath("$[0].longitude").isNumber())
                .andExpect(jsonPath("$[0].regionType").exists())
                .andExpect(jsonPath("$[0].normalTmax").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/heat/points/{locationId}/forecast returns 404 when no forecast exists")
    void shouldReturnNotFoundForUnknownOrUnsyncedLocation() throws Exception {
        mockMvc.perform(get("/api/v1/heat/points/unknown-station/forecast"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/heat/points/{locationId}/forecast returns 7-day forecast with IMD criteria flags")
    void shouldReturnPointForecastWithImdCriteria() throws Exception {
        String locationId = "ganga-patna";
        Instant now = Instant.now();

        // Populate test forecast data for ganga-patna
        runRepository.save(new HeatForecastRunEntity(
                locationId,
                "run-test-1",
                now,
                now,
                3,
                10.6
        ));

        List<HeatForecastDayEntity> testDays = List.of(
                new HeatForecastDayEntity(locationId, "2026-05-20", 41.5, 43.0, 35.0, 36.5, 5.0),
                new HeatForecastDayEntity(locationId, "2026-05-21", 45.5, 47.0, 30.0, 36.7, 8.8),
                new HeatForecastDayEntity(locationId, "2026-05-22", 47.5, 49.0, 28.0, 36.9, 10.6)
        );
        dayRepository.saveAll(testDays);

        mockMvc.perform(get("/api/v1/heat/points/" + locationId + "/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(locationId))
                .andExpect(jsonPath("$.regionType").value("PLAINS"))
                .andExpect(jsonPath("$.isStale").value(false))
                .andExpect(jsonPath("$.days", hasSize(3)))
                // Day 1: Tmax=41.5, departure=5.0 -> Gate met, Heat-Wave day
                .andExpect(jsonPath("$.days[0].tmax").value(41.5))
                .andExpect(jsonPath("$.days[0].regionGateMet").value(true))
                .andExpect(jsonPath("$.days[0].isHeatWaveDay").value(true))
                .andExpect(jsonPath("$.days[0].isSevereHeatWaveDay").value(false))
                // Day 2: Tmax=45.5, departure=8.8 -> Gate met, Severe Heat-Wave day (departure > 6.4)
                .andExpect(jsonPath("$.days[1].tmax").value(45.5))
                .andExpect(jsonPath("$.days[1].isHeatWaveDay").value(true))
                .andExpect(jsonPath("$.days[1].isSevereHeatWaveDay").value(true))
                // Day 3: Tmax=47.5 -> Gate met, Severe Heat-Wave day (Tmax >= 47.0)
                .andExpect(jsonPath("$.days[2].tmax").value(47.5))
                .andExpect(jsonPath("$.days[2].isSevereHeatWaveDay").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/heat/feed-status returns live feed health and failure counters")
    void shouldReturnFeedStatus() throws Exception {
        mockMvc.perform(get("/api/v1/heat/feed-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedName").value("OPEN_METEO_WEATHER"))
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.consecutiveFailures").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/heat/sync-live?locationId={id} syncs live forecast for location")
    void shouldSyncLiveWithQueryParam() throws Exception {
        mockMvc.perform(post("/api/v1/heat/sync-live?locationId=yamuna-delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.syncedCount").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/heat/sync-live/{locationId} syncs live forecast for location via path")
    void shouldSyncLiveWithPath() throws Exception {
        mockMvc.perform(post("/api/v1/heat/sync-live/yamuna-delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.syncedCount").value(1));
    }
}
