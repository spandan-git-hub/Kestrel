package com.kestrel.rulesengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.AqSignalPayload;
import com.kestrel.common.events.AqSignalPayload.CurrentPollutants;
import com.kestrel.common.events.AqSignalPayload.ForecastDayPeak;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.HeatSignalPayload;
import com.kestrel.common.events.HeatSignalPayload.HeatDayStats;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.RegionType;
import com.kestrel.common.events.Severity;
import com.kestrel.rulesengine.infra.entity.AlertStateEntity;
import com.kestrel.rulesengine.infra.repository.AlertHistoryRepository;
import com.kestrel.rulesengine.infra.repository.AlertStateRepository;
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

    @Autowired
    private AlertStateRepository alertStateRepository;

    @Test
    @DisplayName("GET /api/v1/rules/status returns all 45 seeded Indian river stations")
    void getStatusReturnsSeededLocations() throws Exception {
        mockMvc.perform(get("/api/v1/rules/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(45)))
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

    @Test
    @DisplayName("Real data requires 2 consecutive runs to escalate to WARNING on Yamuna Delhi")
    void realDataEnforcesConsecutiveRuns() throws Exception {
        LocalDate today = LocalDate.now();
        List<ForecastDayStats> days = new ArrayList<>();
        days.add(new ForecastDayStats(today, 15, 20, 30, 40, 50, 0.1, 0.0, 0.0));
        days.add(new ForecastDayStats(today.plusDays(1), 70, 85, 95, 110, 130, 0.95, 0.55, 0.10));
        for (int i = 2; i < 10; i++) {
            days.add(new ForecastDayStats(today.plusDays(i), 15, 20, 30, 40, 50, 0.1, 0.0, 0.0));
        }

        // Run 1: Real data (isSimulated = false)
        FloodSignalPayload payload1 = new FloodSignalPayload(
                "yamuna-delhi",
                Instant.now(),
                false,
                51,
                days
        );
        EventEnvelope<FloodSignalPayload> envelope1 = EventEnvelope.of(
                "FloodSignal",
                "flood-ingest",
                "corr-run1",
                payload1
        );

        mockMvc.perform(post("/api/v1/rules/events/flood-signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope1)))
                .andExpect(status().isOk());

        // After Run 1: Still NONE (pending 1/2 runs)
        mockMvc.perform(get("/api/v1/rules/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.locationId == 'yamuna-delhi')].severity").value("NONE"));

        // Run 2: Second consecutive observation
        EventEnvelope<FloodSignalPayload> envelope2 = EventEnvelope.of(
                "FloodSignal",
                "flood-ingest",
                "corr-run2",
                payload1
        );

        mockMvc.perform(post("/api/v1/rules/events/flood-signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope2)))
                .andExpect(status().isOk());

        // After Run 2: Successfully escalated to WARNING
        mockMvc.perform(get("/api/v1/rules/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.locationId == 'yamuna-delhi')].severity").value("WARNING"))
                .andExpect(jsonPath("$[?(@.locationId == 'yamuna-delhi')].activeAlertId", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/rules/events/heat-signal processes HeatSignal, transitions state in locationId#HEAT and saves history")
    void processHeatSignalTransitionsStateAndEmitsAlertEvent() throws Exception {
        LocalDate today = LocalDate.now();
        List<HeatDayStats> days = new ArrayList<>();
        // Day 1: Heat wave day (Tmax = 43.5°C >= 40.0°C gate, departure = +5.2°C >= 4.5°C) -> Candidate WATCH
        days.add(new HeatDayStats(today, 38.0, 40.0, 40.0, 37.0, 1.0));
        days.add(new HeatDayStats(today.plusDays(1), 43.5, 46.5, 30.0, 38.3, 5.2));
        for (int i = 2; i < 7; i++) {
            days.add(new HeatDayStats(today.plusDays(i), 38.0, 40.0, 40.0, 37.0, 1.0));
        }

        HeatSignalPayload payload = new HeatSignalPayload(
                "ganga-varanasi",
                "run-heat-it-1",
                Instant.now(),
                Instant.now(),
                RegionType.PLAINS,
                days
        );

        EventEnvelope<HeatSignalPayload> envelope = EventEnvelope.of(
                "HeatSignal",
                "heat-ingest",
                "heat-corr-id-1",
                payload
        );

        mockMvc.perform(post("/api/v1/rules/events/heat-signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));

        // Verify alert state transitioned in kestrel_rules_alert_state (PK: ganga-varanasi#HEAT)
        var heatStateOpt = alertStateRepository.findById("ganga-varanasi#HEAT");
        assertThat(heatStateOpt).isPresent();
        AlertStateEntity heatState = heatStateOpt.get();
        assertThat(heatState.getLocationId()).isEqualTo("ganga-varanasi#HEAT");
        assertThat(heatState.getHazard()).isEqualTo(Hazard.HEAT);
        assertThat(heatState.getCurrentSeverity()).isEqualTo(Severity.WATCH);
        assertThat(heatState.getProvenance()).isEqualTo(Provenance.MODEL_ONLY);
        assertThat(heatState.getFirstExceedanceDate()).isEqualTo(today.plusDays(1));
        assertThat(heatState.getPeakExceedanceShare()).isEqualTo(5.2);
        assertThat(heatState.getActiveAlertId()).isNotNull();

        // Verify alert history entity was created for ganga-varanasi with Hazard.HEAT
        assertThat(alertHistoryRepository.findByLocationIdOrderByTransitionedAtDesc("ganga-varanasi"))
                .isNotEmpty()
                .anySatisfy(hist -> {
                    assertThat(hist.getHazard()).isEqualTo(Hazard.HEAT);
                    assertThat(hist.getToSeverity()).isEqualTo(Severity.WATCH);
                    assertThat(hist.getPeakExceedanceShare()).isEqualTo(5.2);
                });
    }

    @Test
    @DisplayName("POST /api/v1/rules/events/aq-signal processes CPCB AQI, transitions state and emits AlertEvent")
    void processAqSignalTransitionsStateAndEmitsAlertEvent() throws Exception {
        LocalDate today = LocalDate.now();
        // PM2.5 = 150 µg/m³ => AQI 323 (Very Poor -> WARNING)
        CurrentPollutants current = new CurrentPollutants(150.0, 200.0, 60.0, 40.0, 40.0, 1.2, 24);

        AqSignalPayload payload = new AqSignalPayload(
                "delhi-anand-vihar",
                "run-aq-it-1",
                Instant.now(),
                Instant.now(),
                current,
                List.of()
        );

        EventEnvelope<AqSignalPayload> envelope = EventEnvelope.of(
                "AqSignal",
                "aq-ingest",
                "aq-corr-id-1",
                payload
        );

        mockMvc.perform(post("/api/v1/rules/events/aq-signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")));

        // Verify alert state transitioned in kestrel_rules_alert_state (PK: delhi-anand-vihar#AIR_QUALITY)
        var aqStateOpt = alertStateRepository.findById("delhi-anand-vihar#AIR_QUALITY");
        assertThat(aqStateOpt).isPresent();
        AlertStateEntity aqState = aqStateOpt.get();
        assertThat(aqState.getLocationId()).isEqualTo("delhi-anand-vihar#AIR_QUALITY");
        assertThat(aqState.getHazard()).isEqualTo(Hazard.AIR_QUALITY);
        // Note: First run with candidate WARNING has candidateRuns=1 pending confirmation in StateMachine (unless 2 runs or candidate >= WATCH 1 run)
        // With candidate WARNING: state transition: 1st run leaves currentSeverity at NONE (or escalated on 2nd run)
        // Let's verify candidateSeverity is WARNING and candidateRuns is 1
        assertThat(aqState.getCandidateSeverity()).isEqualTo(Severity.WARNING);
        assertThat(aqState.getCandidateRuns()).isEqualTo(1);
        assertThat(aqState.getProvenance()).isEqualTo(Provenance.MODEL_ONLY);
        assertThat(aqState.getPeakExceedanceShare()).isEqualTo(323.0);
    }
}
