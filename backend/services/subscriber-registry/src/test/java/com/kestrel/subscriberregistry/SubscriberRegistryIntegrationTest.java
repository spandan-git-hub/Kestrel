package com.kestrel.subscriberregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionRequest;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionResponse;
import com.kestrel.subscriberregistry.infra.entity.ConsentLogEntity;
import com.kestrel.subscriberregistry.infra.repository.ConsentLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SubscriberRegistryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsentLogRepository consentLogRepository;

    @Test
    @DisplayName("GET /api/v1/registry/locations returns all 45 real Indian river stations across basins")
    void getLocationsReturnsAll45IndianRiverStations() throws Exception {
        mockMvc.perform(get("/api/v1/registry/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(45)))
                // Verify West Coastal station
                .andExpect(jsonPath("$[?(@.id == 'mumbai-kurla')].name", hasItem("Kurla Ward L, Mumbai")))
                .andExpect(jsonPath("$[?(@.id == 'mumbai-kurla')].basin", hasItem("West Coastal")))
                .andExpect(jsonPath("$[?(@.id == 'mumbai-kurla')].primaryLanguage", hasItem("mr")))
                .andExpect(jsonPath("$[?(@.id == 'mumbai-kurla')].thresholds.twoYear", hasItem(45.0)))
                // Verify Ganga Basin station
                .andExpect(jsonPath("$[?(@.id == 'patna-ward12')].name", hasItem("Ward 12, Patna")))
                .andExpect(jsonPath("$[?(@.id == 'patna-ward12')].basin", hasItem("Ganga Basin")))
                .andExpect(jsonPath("$[?(@.id == 'patna-ward12')].primaryLanguage", hasItem("hi")))
                // Verify Brahmaputra Basin station
                .andExpect(jsonPath("$[?(@.id == 'guwahati-river')].basin", hasItem("Brahmaputra")))
                // Verify Indus/North Basin station
                .andExpect(jsonPath("$[?(@.id == 'jhelum-srinagar')].basin", hasItem("Indus/North")))
                // Verify Godavari Basin station
                .andExpect(jsonPath("$[?(@.id == 'godavari-nashik')].basin", hasItem("Godavari")))
                .andExpect(jsonPath("$[?(@.id == 'godavari-nashik')].primaryLanguage", hasItem("mr")))
                // Verify Krishna Basin station
                .andExpect(jsonPath("$[?(@.id == 'krishna-sangli')].basin", hasItem("Krishna")))
                // Verify Mahanadi Basin station
                .andExpect(jsonPath("$[?(@.id == 'cuttack-delta')].basin", hasItem("Mahanadi")))
                // Verify West-Flowing Basin station
                .andExpect(jsonPath("$[?(@.id == 'narmada-hoshangabad')].basin", hasItem("West-Flowing")))
                // Verify Cauvery/South Basin station
                .andExpect(jsonPath("$[?(@.id == 'periyar-aluva')].basin", hasItem("Cauvery/South")));
    }

    @Test
    @DisplayName("GET /api/v1/registry/locations/{id} returns specific location with basin details or 404")
    void getLocationById() throws Exception {
        mockMvc.perform(get("/api/v1/registry/locations/ganga-haridwar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("ganga-haridwar")))
                .andExpect(jsonPath("$.name", is("Haridwar Ghats")))
                .andExpect(jsonPath("$.state", is("Uttarakhand")))
                .andExpect(jsonPath("$.riverName", is("Ganges River")))
                .andExpect(jsonPath("$.basin", is("Ganga Basin")))
                .andExpect(jsonPath("$.primaryLanguage", is("hi")))
                .andExpect(jsonPath("$.thresholds.twoYear", is(4200.0)))
                .andExpect(jsonPath("$.thresholds.fiveYear", is(6500.0)))
                .andExpect(jsonPath("$.thresholds.twentyYear", is(9800.0)));

        mockMvc.perform(get("/api/v1/registry/locations/non-existent-loc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("DPDP Act 2023: Reject subscription containing phone numbers or email addresses (Zero PII)")
    void rejectSubscriptionWithPii() throws Exception {
        // Indian phone with +91
        CreateSubscriptionRequest req1 = new CreateSubscriptionRequest(
                "mumbai-kurla",
                Channel.TELEGRAM,
                "+919876543210",
                Language.mr,
                Severity.WARNING,
                true,
                "DPDP-2023-v1.0"
        );
        mockMvc.perform(post("/api/v1/registry/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("DPDP Act 2023 compliance violation")));

        // 10-digit Indian mobile number
        CreateSubscriptionRequest req2 = new CreateSubscriptionRequest(
                "mumbai-kurla",
                Channel.TELEGRAM,
                "9876543210",
                Language.mr,
                Severity.WARNING,
                true,
                "DPDP-2023-v1.0"
        );
        mockMvc.perform(post("/api/v1/registry/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("DPDP Act 2023 compliance violation")));

        // Email address
        CreateSubscriptionRequest req3 = new CreateSubscriptionRequest(
                "mumbai-kurla",
                Channel.TELEGRAM,
                "resident@example.com",
                Language.mr,
                Severity.WARNING,
                true,
                "DPDP-2023-v1.0"
        );
        mockMvc.perform(post("/api/v1/registry/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("DPDP Act 2023 compliance violation")));
    }

    @Test
    @DisplayName("DPDP Act 2023: Reject subscription without opt-in consent")
    void rejectSubscriptionWithoutConsent() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(
                "mumbai-kurla",
                Channel.TELEGRAM,
                "tg_chat_81238",
                Language.mr,
                Severity.WARNING,
                false,
                "DPDP-2023-v1.0"
        );
        mockMvc.perform(post("/api/v1/registry/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Explicit opt-in consent is required")));
    }

    @Test
    @DisplayName("Subscription lifecycle with DPDP consent audit log: CREATE -> GET -> LIST -> DELETE (Erasure)")
    void subscriptionCrudLifecycleWithDpdpAudit() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "mumbai-kurla",
                Channel.TELEGRAM,
                "98127391",
                Language.mr,
                Severity.WARNING,
                true,
                "DPDP-2023-v1.0"
        );

        // 1. CREATE
        MvcResult createResult = mockMvc.perform(post("/api/v1/registry/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.message", is("Subscription registered.")))
                .andReturn();

        CreateSubscriptionResponse response = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                CreateSubscriptionResponse.class
        );
        String token = response.token();

        // Verify DPDP consent log audit entry was persisted
        List<ConsentLogEntity> consentLogs = consentLogRepository.findByToken(token);
        assertThat(consentLogs).isNotEmpty();
        assertThat(consentLogs.get(0).getStatus()).isEqualTo("GRANTED");
        assertThat(consentLogs.get(0).getConsentVersion()).isEqualTo("DPDP-2023-v1.0");

        // 2. GET by /subscriptions/{token}
        mockMvc.perform(get("/api/v1/registry/subscriptions/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(token)))
                .andExpect(jsonPath("$.locationId", is("mumbai-kurla")))
                .andExpect(jsonPath("$.channel", is("TELEGRAM")))
                .andExpect(jsonPath("$.recipientIdentifier", is("98127391")))
                .andExpect(jsonPath("$.recipientHandle", is("98127391")))
                .andExpect(jsonPath("$.language", is("mr")))
                .andExpect(jsonPath("$.preferredLanguage", is("mr")))
                .andExpect(jsonPath("$.minSeverity", is("WARNING")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.active", is(true)));

        // 3. GET by /subscriptions/token/{token}
        mockMvc.perform(get("/api/v1/registry/subscriptions/token/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(token)));

        // 4. GET all by locationId
        mockMvc.perform(get("/api/v1/registry/subscriptions?locationId=mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));

        // 5. DELETE by /subscriptions/token/{token} (DPDP Act 2023 Right to Hard Erasure)
        mockMvc.perform(delete("/api/v1/registry/subscriptions/token/" + token))
                .andExpect(status().isNoContent());

        // Verify consent log was updated to WITHDRAWN
        List<ConsentLogEntity> updatedConsent = consentLogRepository.findByToken(token);
        assertThat(updatedConsent).isNotEmpty();
        assertThat(updatedConsent.get(0).getStatus()).isEqualTo("WITHDRAWN");
        assertThat(updatedConsent.get(0).getWithdrawnAt()).isNotNull();

        // 6. GET by token again returns 404 (Erased)
        mockMvc.perform(get("/api/v1/registry/subscriptions/" + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("POST /api/v1/registry/subscriptions rejects non-existent location")
    void rejectSubscriptionForUnknownLocation() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "unknown-city",
                Channel.TELEGRAM,
                "tg_resident_123",
                Language.en,
                Severity.WARNING,
                true,
                "DPDP-2023-v1.0"
        );

        mockMvc.perform(post("/api/v1/registry/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("POST /api/v1/registry/locations/sync triggers re-broadcast of all 45 stations")
    void syncLocationsRebroadcasts() throws Exception {
        mockMvc.perform(post("/api/v1/registry/locations/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SYNCED")))
                .andExpect(jsonPath("$.syncedLocationsCount", is(45)));
    }
}
