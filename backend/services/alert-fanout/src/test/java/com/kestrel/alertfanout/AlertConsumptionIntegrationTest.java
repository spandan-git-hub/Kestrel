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
}
