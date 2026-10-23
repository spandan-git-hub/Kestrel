package com.kestrel.subscriberregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionRequest;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @Test
    @DisplayName("GET /api/v1/registry/locations returns 4 seeded pilot locations")
    void getLocationsReturnsSeededPilotLocations() throws Exception {
        mockMvc.perform(get("/api/v1/registry/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id", is("mumbai-kurla")))
                .andExpect(jsonPath("$[0].name", is("Kurla Ward L, Mumbai")))
                .andExpect(jsonPath("$[0].state", is("Maharashtra")))
                .andExpect(jsonPath("$[0].riverName", is("Mithi River")))
                .andExpect(jsonPath("$[0].primaryLanguage", is("mr")))
                .andExpect(jsonPath("$[0].thresholds.twoYear", is(45.0)))
                .andExpect(jsonPath("$[0].thresholds.fiveYear", is(85.0)))
                .andExpect(jsonPath("$[0].thresholds.twentyYear", is(140.0)))
                .andExpect(jsonPath("$[0].active", is(true)))
                .andExpect(jsonPath("$[1].id", is("patna-ward12")))
                .andExpect(jsonPath("$[1].primaryLanguage", is("hi")))
                .andExpect(jsonPath("$[2].id", is("guwahati-river")))
                .andExpect(jsonPath("$[3].id", is("cuttack-delta")));
    }

    @Test
    @DisplayName("GET /api/v1/registry/locations/{id} returns specific location or 404")
    void getLocationById() throws Exception {
        mockMvc.perform(get("/api/v1/registry/locations/patna-ward12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("patna-ward12")))
                .andExpect(jsonPath("$.name", is("Ward 12, Patna")))
                .andExpect(jsonPath("$.riverName", is("Ganges River")))
                .andExpect(jsonPath("$.primaryLanguage", is("hi")));

        mockMvc.perform(get("/api/v1/registry/locations/non-existent-loc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("Subscription lifecycle: CREATE -> GET -> LIST -> DELETE -> GET 404")
    void subscriptionCrudLifecycle() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "mumbai-kurla",
                Channel.TELEGRAM,
                "98127391",
                Language.mr,
                Severity.WARNING
        );

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

        // 2. GET by token
        mockMvc.perform(get("/api/v1/registry/subscriptions/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(token)))
                .andExpect(jsonPath("$.locationId", is("mumbai-kurla")))
                .andExpect(jsonPath("$.channel", is("TELEGRAM")))
                .andExpect(jsonPath("$.recipientIdentifier", is("98127391")))
                .andExpect(jsonPath("$.language", is("mr")))
                .andExpect(jsonPath("$.minSeverity", is("WARNING")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 3. GET all by locationId
        mockMvc.perform(get("/api/v1/registry/subscriptions?locationId=mumbai-kurla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));

        // 4. DELETE by token
        mockMvc.perform(delete("/api/v1/registry/subscriptions/" + token))
                .andExpect(status().isNoContent());

        // 5. GET by token again returns 404
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
                "123456",
                Language.en,
                Severity.WARNING
        );

        mockMvc.perform(post("/api/v1/registry/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("POST /api/v1/registry/locations/sync triggers re-broadcast")
    void syncLocationsRebroadcasts() throws Exception {
        mockMvc.perform(post("/api/v1/registry/locations/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SYNCED")))
                .andExpect(jsonPath("$.syncedLocationsCount", is(4)));
    }
}
