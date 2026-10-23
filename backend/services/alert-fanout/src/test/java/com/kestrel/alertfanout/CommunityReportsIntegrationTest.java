package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.api.dto.CreateReportRequest;
import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.repository.ActiveAlertRepository;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.ReportCategory;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommunityReportsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationReplicaRepository locationReplicaRepository;

    @Autowired
    private ActiveAlertRepository activeAlertRepository;

    @AfterEach
    void tearDown() {
        locationReplicaRepository.findById("mumbai-kurla").ifPresent(loc -> {
            loc.setCurrentProvenance(Provenance.MODEL_ONLY);
            loc.setReportCount(0);
            locationReplicaRepository.save(loc);
        });
        activeAlertRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/fanout/reports accepts ground report and immediately upgrades active alert provenance to MODEL_PLUS_REPORTS")
    void shouldAcceptGroundReportAndUpgradeProvenance() throws Exception {
        // Step 1: Simulate active alert with MODEL_ONLY provenance
        AlertEventPayload alertPayload = new AlertEventPayload(
                "alert-mumbai-test-rep",
                "mumbai-kurla",
                Hazard.FLOOD,
                "DECISION",
                Severity.NONE,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                LocalDate.of(2026, 10, 24),
                0.72,
                true
        );
        EventEnvelope<AlertEventPayload> alertEnvelope = EventEnvelope.of(
                "AlertEvent",
                "rules-engine",
                "corr-test-rep-01",
                alertPayload
        );
        mockMvc.perform(post("/api/v1/fanout/events/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alertEnvelope)))
                .andExpect(status().isOk());

        ActiveAlertEntity beforeReport = activeAlertRepository.findByLocationId("mumbai-kurla").orElseThrow();
        assertThat(beforeReport.getProvenance()).isEqualTo(Provenance.MODEL_ONLY);

        // Step 2: Resident submits ground report
        CreateReportRequest reportRequest = new CreateReportRequest(
                "mumbai-kurla",
                ReportCategory.WATER_RISING,
                "Water rising rapidly on CST Road near railway bridge",
                "Resident Kurla"
        );

        mockMvc.perform(post("/api/v1/fanout/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationId").value("mumbai-kurla"))
                .andExpect(jsonPath("$.category").value("WATER_RISING"))
                .andExpect(jsonPath("$.newProvenance").value("MODEL_PLUS_REPORTS"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Provenance upgraded")));

        // Step 3: Verify LocationReplica provenance and report count updated
        LocationReplica replica = locationReplicaRepository.findById("mumbai-kurla").orElseThrow();
        assertThat(replica.getCurrentProvenance()).isEqualTo(Provenance.MODEL_PLUS_REPORTS);
        assertThat(replica.getReportCount()).isGreaterThanOrEqualTo(1);

        // Step 4: Verify ActiveAlert provenance and text recomposed with new disclaimer
        ActiveAlertEntity afterReport = activeAlertRepository.findByLocationId("mumbai-kurla").orElseThrow();
        assertThat(afterReport.getProvenance()).isEqualTo(Provenance.MODEL_PLUS_REPORTS);
        assertThat(afterReport.getComposedEnglish()).contains("Model Guidance + Community Reports");
        assertThat(afterReport.getComposedHindi()).contains("मॉडल अनुमान + स्थानीय जन रिपोर्ट");
        assertThat(afterReport.getComposedMarathi()).contains("मॉडेल अंदाज + स्थानिक नागरिकांचे अहवाल");

        // Step 5: Verify GET /api/v1/fanout/reports/{locationId}
        mockMvc.perform(get("/api/v1/fanout/reports/mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].category").value("WATER_RISING"))
                .andExpect(jsonPath("$[0].description").value("Water rising rapidly on CST Road near railway bridge"));
    }
}
