package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.repository.ActiveAlertRepository;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlertConsumptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActiveAlertRepository activeAlertRepository;

    @Test
    @DisplayName("POST /api/v1/fanout/events/alert consumes DECISION AlertEvent and composes multilingual templates")
    void shouldConsumeAlertEventAndComposeTemplates() throws Exception {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-mumbai-001",
                "mumbai-kurla",
                Hazard.FLOOD,
                "DECISION",
                Severity.NONE,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 10, 24),
                0.68,
                true
        );

        EventEnvelope<AlertEventPayload> envelope = EventEnvelope.of(
                "AlertEvent",
                "rules-engine",
                "corr-test-fanout-alert-01",
                payload
        );

        mockMvc.perform(post("/api/v1/fanout/events/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.alertId").value("alert-mumbai-001"))
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.severity").value("WARNING"));

        Optional<ActiveAlertEntity> activeOpt = activeAlertRepository.findByLocationId("mumbai-kurla");
        assertThat(activeOpt).isPresent();
        ActiveAlertEntity active = activeOpt.get();
        assertThat(active.getAlertId()).isEqualTo("alert-mumbai-001");
        assertThat(active.getToSeverity()).isEqualTo(Severity.WARNING);
        assertThat(active.getComposedEnglish()).contains("⚠️ KESTREL FLOOD WARNING: Kurla Ward L, Mumbai");
        assertThat(active.getComposedEnglish()).contains("Provenance: Model Guidance Only — Not an official warning. Confirm locally.");
        assertThat(active.getComposedHindi()).contains("⚠️ केस्ट्रल बाढ़ चेतावनी: Kurla Ward L, Mumbai");
        assertThat(active.getComposedMarathi()).contains("⚠️ केस्ट्रल पूर इशारा: Kurla Ward L, Mumbai");
        assertThat(active.getWhatsAppUrl()).startsWith("https://wa.me/?text=");
    }

    @Test
    @DisplayName("PT-FANOUT-MH-01 & PT-FANOUT-MH-02: Consumes Heat AlertEvent and composes heat templates")
    void shouldConsumeHeatAlertEvent() throws Exception {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-heat-delhi-01",
                "delhi-central",
                Hazard.HEAT,
                "DECISION",
                Severity.WATCH,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 11, 2),
                0.85,
                true
        );

        EventEnvelope<AlertEventPayload> envelope = EventEnvelope.of(
                "AlertEvent",
                "rules-engine",
                "corr-test-fanout-heat-01",
                payload
        );

        mockMvc.perform(post("/api/v1/fanout/events/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.alertId").value("alert-heat-delhi-01"))
                .andExpect(jsonPath("$.locationId").value("delhi-central"))
                .andExpect(jsonPath("$.severity").value("WARNING"));

        Optional<ActiveAlertEntity> activeOpt = activeAlertRepository.findByLocationId("delhi-central");
        assertThat(activeOpt).isPresent();
        ActiveAlertEntity active = activeOpt.get();
        assertThat(active.getAlertId()).isEqualTo("alert-heat-delhi-01");
        assertThat(active.getHazard()).isEqualTo(Hazard.HEAT);
        assertThat(active.getComposedEnglish()).contains("HEAT WAVE WARNING");
        assertThat(active.getComposedEnglish()).contains("Avoid going out between 12 noon and 3 PM; drink water frequently");
        assertThat(active.getComposedEnglish()).contains("Guidance based on numerical model forecasts");
        assertThat(active.getComposedHindi()).contains("लू चेतावनी");
        assertThat(active.getWhatsAppUrl()).contains("HEAT+WAVE+ALERT");
    }

    @Test
    @DisplayName("PT-FANOUT-MH-01 & PT-FANOUT-MH-02: Consumes Air Quality AlertEvent and composes AQ templates")
    void shouldConsumeAqAlertEvent() throws Exception {
        AlertEventPayload payload = new AlertEventPayload(
                "alert-aq-delhi-01",
                "delhi-anand-vihar",
                Hazard.AIR_QUALITY,
                "DECISION",
                Severity.WARNING,
                Severity.SEVERE,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 11, 2),
                0.95,
                true
        );

        EventEnvelope<AlertEventPayload> envelope = EventEnvelope.of(
                "AlertEvent",
                "rules-engine",
                "corr-test-fanout-aq-01",
                payload
        );

        mockMvc.perform(post("/api/v1/fanout/events/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.alertId").value("alert-aq-delhi-01"))
                .andExpect(jsonPath("$.locationId").value("delhi-anand-vihar"))
                .andExpect(jsonPath("$.severity").value("SEVERE"));

        Optional<ActiveAlertEntity> activeOpt = activeAlertRepository.findByLocationId("delhi-anand-vihar");
        assertThat(activeOpt).isPresent();
        ActiveAlertEntity active = activeOpt.get();
        assertThat(active.getAlertId()).isEqualTo("alert-aq-delhi-01");
        assertThat(active.getHazard()).isEqualTo(Hazard.AIR_QUALITY);
        assertThat(active.getComposedEnglish()).contains("SEVERE AIR QUALITY EMERGENCY");
        assertThat(active.getComposedEnglish()).contains("seek immediate medical help for breathing difficulties");
        assertThat(active.getComposedEnglish()).contains("Guidance based on numerical model forecasts");
        assertThat(active.getComposedHindi()).contains("गंभीर वायु गुणवत्ता आपातकाल");
        assertThat(active.getWhatsAppUrl()).contains("AIR+QUALITY+ALERT");
    }
}
