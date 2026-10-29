package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.app.SubscriberReplicaService;
import com.kestrel.alertfanout.consumer.FanoutRegistryQueueConsumer;
import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.alertfanout.infra.repository.SubscriberReplicaRepository;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import com.kestrel.common.events.SubscriptionChangedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriberReplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriberReplicaRepository replicaRepository;

    @Autowired
    private SubscriberReplicaService subscriberReplicaService;

    @Autowired
    private FanoutRegistryQueueConsumer registryConsumer;

    @BeforeEach
    void setUp() {
        replicaRepository.deleteAll();
    }

    @Test
    @DisplayName("PT-FANOUT-01: Replicate subscription via HTTP event endpoint")
    void shouldReplicateSubscriptionViaHttp() throws Exception {
        String token = UUID.randomUUID().toString();
        SubscriptionChangedPayload payload = new SubscriptionChangedPayload(
                token,
                "patna-ward12",
                Channel.TELEGRAM,
                "tg-chat-998877",
                Language.hi,
                Severity.WATCH,
                "CREATED"
        );
        EventEnvelope<SubscriptionChangedPayload> envelope = EventEnvelope.of(
                "SubscriptionChanged",
                "subscriber-registry",
                "corr-test-01",
                payload
        );

        mockMvc.perform(post("/api/v1/fanout/events/subscription-changed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLICATED"))
                .andExpect(jsonPath("$.token").value(token));

        Optional<SubscriberReplica> saved = replicaRepository.findByLocationIdAndToken("patna-ward12", token);
        assertThat(saved).isPresent();
        assertThat(saved.get().getChannel()).isEqualTo(Channel.TELEGRAM);
        assertThat(saved.get().getRecipientIdentifier()).isEqualTo("tg-chat-998877");
        assertThat(saved.get().getLanguage()).isEqualTo(Language.hi);
        assertThat(saved.get().getMinSeverity()).isEqualTo(Severity.WATCH);
        assertThat(saved.get().getStatus()).isEqualTo("ACTIVE");

        // Verify GET /api/v1/fanout/events/subscribers/{locationId}
        mockMvc.perform(get("/api/v1/fanout/events/subscribers/patna-ward12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].token").value(token));
    }

    @Test
    @DisplayName("PT-FANOUT-01: Replicate and delete subscription via SQS message consumption")
    void shouldReplicateAndDeleteViaSqsConsumer() {
        String token = UUID.randomUUID().toString();

        // 1. Consume CREATED event directly
        SubscriptionChangedPayload createPayload = new SubscriptionChangedPayload(
                token,
                "mumbai-kurla",
                Channel.WEB_PUSH,
                "push-endpoint-key-12345",
                Language.mr,
                Severity.WARNING,
                "CREATED"
        );
        EventEnvelope<SubscriptionChangedPayload> createEnvelope = EventEnvelope.of(
                "SubscriptionChanged",
                "subscriber-registry",
                "corr-test-create",
                createPayload
        );

        registryConsumer.processMessage(assertSerialize(createEnvelope));

        Optional<SubscriberReplica> replica = replicaRepository.findByLocationIdAndToken("mumbai-kurla", token);
        assertThat(replica).isPresent();
        assertThat(replica.get().getChannel()).isEqualTo(Channel.WEB_PUSH);
        assertThat(replica.get().getMinSeverity()).isEqualTo(Severity.WARNING);

        // 2. Consume DELETED event wrapped in SNS notification envelope
        SubscriptionChangedPayload deletePayload = new SubscriptionChangedPayload(
                token,
                "mumbai-kurla",
                Channel.WEB_PUSH,
                "push-endpoint-key-12345",
                Language.mr,
                Severity.WARNING,
                "DELETED"
        );
        EventEnvelope<SubscriptionChangedPayload> deleteEnvelope = EventEnvelope.of(
                "SubscriptionChanged",
                "subscriber-registry",
                "corr-test-delete",
                deletePayload
        );
        String snsNotificationJson = String.format("{\"Type\":\"Notification\",\"Message\":%s}",
                assertSerialize(assertSerialize(deleteEnvelope)));

        registryConsumer.processMessage(snsNotificationJson);

        Optional<SubscriberReplica> deleted = replicaRepository.findByLocationIdAndToken("mumbai-kurla", token);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("PT-FANOUT-01: Filter active subscribers by location and severity threshold")
    void shouldFilterActiveSubscribersByLocationAndSeverityThreshold() {
        String loc = "guwahati-river";

        // Resident A: WATCH threshold
        subscriberReplicaService.replicateSubscription(new SubscriptionChangedPayload(
                "tok-watch", loc, Channel.TELEGRAM, "chat-watch", Language.en, Severity.WATCH, "CREATED"));

        // Resident B: WARNING threshold
        subscriberReplicaService.replicateSubscription(new SubscriptionChangedPayload(
                "tok-warning", loc, Channel.WEB_PUSH, "push-warning", Language.en, Severity.WARNING, "CREATED"));

        // Resident C: SEVERE threshold
        subscriberReplicaService.replicateSubscription(new SubscriptionChangedPayload(
                "tok-severe", loc, Channel.TELEGRAM, "chat-severe", Language.en, Severity.SEVERE, "CREATED"));

        // Resident D: WATCH threshold but BLOCKED
        subscriberReplicaService.replicateSubscription(new SubscriptionChangedPayload(
                "tok-blocked", loc, Channel.TELEGRAM, "chat-blocked", Language.en, Severity.WATCH, "CREATED"));
        subscriberReplicaService.updateSubscriberStatus(loc, "tok-blocked", "BLOCKED");

        // Resident E: in another location (cuttack-delta)
        subscriberReplicaService.replicateSubscription(new SubscriptionChangedPayload(
                "tok-cuttack", "cuttack-delta", Channel.TELEGRAM, "chat-cuttack", Language.en, Severity.WATCH, "CREATED"));

        // 1. Alert is NONE -> 0 recipients
        List<SubscriberReplica> noneRecipients = subscriberReplicaService.getActiveSubscribersForAlert(loc, Severity.NONE);
        assertThat(noneRecipients).isEmpty();

        // 2. Alert is WATCH -> Only Resident A (Resident D is blocked)
        List<SubscriberReplica> watchRecipients = subscriberReplicaService.getActiveSubscribersForAlert(loc, Severity.WATCH);
        assertThat(watchRecipients).extracting(SubscriberReplica::getToken)
                .containsExactly("tok-watch");

        // 3. Alert is WARNING -> Residents A and B
        List<SubscriberReplica> warningRecipients = subscriberReplicaService.getActiveSubscribersForAlert(loc, Severity.WARNING);
        assertThat(warningRecipients).extracting(SubscriberReplica::getToken)
                .containsExactlyInAnyOrder("tok-watch", "tok-warning");

        // 4. Alert is SEVERE -> Residents A, B, and C (all active, threshold satisfied)
        List<SubscriberReplica> severeRecipients = subscriberReplicaService.getActiveSubscribersForAlert(loc, Severity.SEVERE);
        assertThat(severeRecipients).extracting(SubscriberReplica::getToken)
                .containsExactlyInAnyOrder("tok-watch", "tok-warning", "tok-severe");
    }

    private String assertSerialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

