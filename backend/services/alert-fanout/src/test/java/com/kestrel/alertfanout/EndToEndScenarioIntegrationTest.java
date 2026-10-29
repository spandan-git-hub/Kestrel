package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.api.dto.BroadcastRequest;
import com.kestrel.alertfanout.api.dto.CreateReportRequest;
import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.repository.ActiveAlertRepository;
import com.kestrel.alertfanout.infra.repository.CommunityReportRepository;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.ReportCategory;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PT4-02: Automated End-to-End Integration Test
 *
 * Simulates the complete scenario lifecycle:
 * 1. Location replication for Kurla Ward L, Mumbai
 * 2. 5-Year Flood simulation signal -> transitions state to WARNING (stage=DECISION, MODEL_ONLY)
 * 3. Resident submits ground report -> provenance immediately upgrades to MODEL_PLUS_REPORTS
 * 4. Coordinator triggers one-tap broadcast -> dispatches to Telegram and Web Push workers
 * 5. Verify dispatch logged in fanout DynamoDB delivery table and accessible via REST API
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndToEndScenarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationReplicaRepository locationReplicaRepository;

    @Autowired
    private ActiveAlertRepository activeAlertRepository;

    @Autowired
    private CommunityReportRepository communityReportRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @BeforeEach
    void setUp() {
        communityReportRepository.deleteAll();
        deliveryLogRepository.deleteAll();
        activeAlertRepository.deleteAll();

        // Seed / reset Kurla Ward L location replica
        LocationReplica kurla = locationReplicaRepository.findById("mumbai-kurla").orElseGet(LocationReplica::new);
        kurla.setLocationId("mumbai-kurla");
        kurla.setName("Kurla Ward L, Mumbai");
        kurla.setState("Maharashtra");
        kurla.setLatitude(19.0726);
        kurla.setLongitude(72.8797);
        kurla.setRiverName("Mithi River");
        kurla.setThreshold2yr(45.0);
        kurla.setThreshold5yr(85.0);
        kurla.setThreshold20yr(140.0);
        kurla.setPrimaryLanguage(Language.mr);
        kurla.setCurrentProvenance(Provenance.MODEL_ONLY);
        kurla.setReportCount(0);
        kurla.setActive(true);
        locationReplicaRepository.save(kurla);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        locationReplicaRepository.findById("mumbai-kurla").ifPresent(loc -> {
            loc.setCurrentProvenance(Provenance.MODEL_ONLY);
            loc.setReportCount(0);
            locationReplicaRepository.save(loc);
        });
        activeAlertRepository.deleteAll();
        communityReportRepository.deleteAll();
        deliveryLogRepository.deleteAll();
    }

    @Test
    @DisplayName("PT4-02: Full E2E Flow: 5-Yr Flood Warning -> Ground Report Upgrade -> Broadcast -> Mock Queue Verified")
    void executeFullE2EScenario() throws Exception {
        final String locationId = "mumbai-kurla";
        final LocalDate forecastDate = LocalDate.now().plusDays(1);

        // =====================================================================
        // STEP 1: Ingest AlertEvent representing Rules Engine transition to WARNING
        // =====================================================================
        AlertEventPayload alertPayload = new AlertEventPayload(
                "alert-kurla-e2e-001",
                locationId,
                Hazard.FLOOD,
                "DECISION",
                Severity.NONE,
                Severity.WARNING,
                Provenance.MODEL_ONLY,
                Instant.now(),
                forecastDate,
                0.68,
                true
        );

        EventEnvelope<AlertEventPayload> alertEnvelope = EventEnvelope.of(
                "AlertEvent",
                "rules-engine",
                "corr-e2e-stage1",
                alertPayload
        );

        mockMvc.perform(post("/api/v1/fanout/events/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alertEnvelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PROCESSED")))
                .andExpect(jsonPath("$.severity", is("WARNING")));

        // Verify ActiveAlert was created with WARNING and MODEL_ONLY
        ActiveAlertEntity initialAlert = activeAlertRepository.findByLocationId(locationId)
                .orElseThrow(() -> new AssertionError("ActiveAlert not found for " + locationId));
        assertThat(initialAlert.getToSeverity()).isEqualTo(Severity.WARNING);
        assertThat(initialAlert.getProvenance()).isEqualTo(Provenance.MODEL_ONLY);
        assertThat(initialAlert.getComposedEnglish()).contains("Model Guidance Only");

        // Verify public endpoint returns active alert
        mockMvc.perform(get("/api/v1/fanout/active/" + locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId", is(locationId)))
                .andExpect(jsonPath("$.toSeverity", is("WARNING")))
                .andExpect(jsonPath("$.provenance", is("MODEL_ONLY")));

        // =====================================================================
        // STEP 2: Resident submits community ground report
        // =====================================================================
        CreateReportRequest reportRequest = new CreateReportRequest(
                locationId,
                ReportCategory.WATER_RISING,
                "Water rising rapidly along CST Road and Mithi riverbank near Kurla station.",
                "Resident Ward L"
        );

        mockMvc.perform(post("/api/v1/fanout/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationId", is(locationId)))
                .andExpect(jsonPath("$.category", is("WATER_RISING")))
                .andExpect(jsonPath("$.newProvenance", is("MODEL_PLUS_REPORTS")));

        // Verify provenance immediately upgraded across replica and active alert
        LocationReplica upgradedReplica = locationReplicaRepository.findById(locationId).orElseThrow();
        assertThat(upgradedReplica.getCurrentProvenance()).isEqualTo(Provenance.MODEL_PLUS_REPORTS);
        assertThat(upgradedReplica.getReportCount()).isEqualTo(1);

        ActiveAlertEntity upgradedAlert = activeAlertRepository.findByLocationId(locationId).orElseThrow();
        assertThat(upgradedAlert.getProvenance()).isEqualTo(Provenance.MODEL_PLUS_REPORTS);
        // Verify trilingual recomposed messages have upgraded provenance text
        assertThat(upgradedAlert.getComposedEnglish()).contains("Model Guidance + Community Reports");
        assertThat(upgradedAlert.getComposedHindi()).contains("मॉडल अनुमान + स्थानीय जन रिपोर्ट");
        assertThat(upgradedAlert.getComposedMarathi()).contains("मॉडेल अंदाज + स्थानिक नागरिकांचे अहवाल");
        assertThat(upgradedAlert.getWhatsAppUrl()).startsWith("https://wa.me/?text=");

        // Verify reports endpoint returns the submitted report
        mockMvc.perform(get("/api/v1/fanout/reports/" + locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category", is("WATER_RISING")))
                .andExpect(jsonPath("$[0].reporterAlias", is("Resident Ward L")));

        // =====================================================================
        // STEP 3: Coordinator triggers one-tap broadcast
        // =====================================================================
        BroadcastRequest broadcastRequest = new BroadcastRequest(
                locationId,
                Hazard.FLOOD,
                Severity.WARNING,
                "Ground report corroborated: water rising on CST Road. Evacuate basement areas.",
                List.of(Channel.TELEGRAM, Channel.WEB_PUSH)
        );

        mockMvc.perform(post("/api/v1/fanout/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broadcastRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broadcastId").exists())
                .andExpect(jsonPath("$.locationId", is(locationId)))
                .andExpect(jsonPath("$.severity", is("WARNING")))
                .andExpect(jsonPath("$.status", is("DISPATCHED")))
                .andExpect(jsonPath("$.dispatchedCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.whatsAppShareUrl", startsWith("https://wa.me/?text=")));

        // =====================================================================
        // STEP 4: Verify dispatch is logged in DynamoDB and accessible via /recent
        // =====================================================================
        mockMvc.perform(get("/api/v1/fanout/recent?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].locationId", is(locationId)))
                .andExpect(jsonPath("$[0].severity", is("WARNING")))
                .andExpect(jsonPath("$[0].provenance", is("MODEL_PLUS_REPORTS")));

        // Verify real delivery receipts in DynamoDB
        List<DeliveryLog> deliveryLogs = deliveryLogRepository.findByLocationIdOrderByTimestampDesc(locationId);
        assertThat(deliveryLogs)
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(2)
                .anySatisfy(entry -> {
                    assertThat(entry.getLocationId()).isEqualTo(locationId);
                    assertThat(entry.getSeverity()).isEqualTo(Severity.WARNING);
                    assertThat(entry.getChannel()).isEqualTo(Channel.TELEGRAM);
                })
                .anySatisfy(entry -> {
                    assertThat(entry.getLocationId()).isEqualTo(locationId);
                    assertThat(entry.getSeverity()).isEqualTo(Severity.WARNING);
                    assertThat(entry.getChannel()).isEqualTo(Channel.WEB_PUSH);
                });
    }
}
