package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.consumer.TelegramDeliveryQueueConsumer;
import com.kestrel.alertfanout.app.SubscriberReplicaService;
import com.kestrel.alertfanout.infra.client.TelegramBotClient;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.alertfanout.infra.lambda.AlertFanoutQueueFunction;
import com.kestrel.alertfanout.infra.limiter.InMemoryTelegramRateLimiter;
import com.kestrel.alertfanout.infra.limiter.TelegramRateLimiter;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.alertfanout.infra.repository.SubscriberReplicaRepository;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import com.kestrel.common.events.SubscriptionChangedPayload;
import com.kestrel.common.events.TelegramDeliveryPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TelegramWorkerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelegramDeliveryQueueConsumer telegramDeliveryConsumer;

    @Autowired
    private SubscriberReplicaService subscriberReplicaService;

    @Autowired
    private SubscriberReplicaRepository subscriberReplicaRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private TelegramRateLimiter telegramRateLimiter;

    @Autowired
    private AlertFanoutQueueFunction alertFanoutQueueFunction;

    @BeforeEach
    void setUp() {
        deliveryLogRepository.deleteAll();
        subscriberReplicaRepository.deleteAll();
        if (telegramRateLimiter instanceof InMemoryTelegramRateLimiter inMem) {
            inMem.reset();
        }
    }

    @Test
    @DisplayName("PT-FANOUT-02: Consumes Telegram delivery payload, delivers alert, and records delivery receipt")
    void shouldProcessTelegramDeliveryAndRecordReceipt() {
        TelegramDeliveryPayload payload = new TelegramDeliveryPayload(
                "deliv-tg-001",
                "alert-patna-100",
                "patna-ward12",
                "Ward 12, Patna",
                Severity.SEVERE,
                "987654321",
                "sub-token-patna",
                Language.hi,
                "चेतावनी: गंगा नदी में बाढ़ की आशंका!",
                Provenance.MODEL_ONLY,
                "https://wa.me/?text=alert",
                Instant.now()
        );

        DeliveryLog result = telegramDeliveryConsumer.deliver(payload);

        assertThat(result).isNotNull();
        assertThat(result.getDeliveryId()).isEqualTo("deliv-tg-001");
        assertThat(result.getAlertId()).isEqualTo("alert-patna-100");
        assertThat(result.getLocationId()).isEqualTo("patna-ward12");
        assertThat(result.getChannel()).isEqualTo(Channel.TELEGRAM);
        assertThat(result.getRecipient()).isEqualTo("987654321");
        assertThat(result.getLanguage()).isEqualTo(Language.hi);
        assertThat(result.getStatus()).isEqualTo("SIMULATED"); // default no bot token in tests
        assertThat(result.getMessageExcerpt()).contains("गंगा नदी");

        // Verify receipt persisted in repository
        List<DeliveryLog> receipts = deliveryLogRepository.findByAlertId("alert-patna-100");
        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).getDeliveryId()).isEqualTo("deliv-tg-001");
    }

    @Test
    @DisplayName("PT-FANOUT-02: Rate limiter enforces 25 msg/s target and rejects requests past 30 msg/s ceiling")
    void shouldEnforceRateLimitCeilingAt30MsgPerSecond() throws InterruptedException {
        // Align to near the beginning of the second window to prevent test clock roll-over
        while (System.currentTimeMillis() % 1000 > 150) {
            Thread.sleep(10);
        }

        // First 30 permits in current second should succeed
        for (int i = 0; i < 30; i++) {
            boolean acquired = telegramRateLimiter.tryAcquire();
            assertThat(acquired).as("Permit " + (i + 1) + " should be acquired within ceiling").isTrue();
        }

        // 31st request exceeds the ceiling
        boolean exceeded = telegramRateLimiter.tryAcquire();
        assertThat(exceeded).as("31st permit should exceed the 30 msg/s ceiling").isFalse();

        // Status check
        TelegramRateLimiter.RateLimitStatus status = telegramRateLimiter.getStatus();
        assertThat(status.currentPerSecondCount()).isGreaterThanOrEqualTo(30);
        assertThat(status.ceilingLimit()).isEqualTo(30);
        assertThat(status.allowed()).isFalse();

        // Test delivery fails with rate limit exception when ceiling is exceeded
        TelegramDeliveryPayload payload = new TelegramDeliveryPayload(
                "deliv-rate-limited",
                "alert-mumbai-200",
                "mumbai-kurla",
                "Kurla Ward L, Mumbai",
                Severity.WARNING,
                "555555555",
                "sub-token-mumbai",
                Language.mr,
                "सावधान: मिठी नदी पाणी पातळी वाढत आहे",
                Provenance.MODEL_ONLY,
                "https://wa.me/?text=alert",
                Instant.now()
        );

        assertThatThrownBy(() -> telegramDeliveryConsumer.deliver(payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Telegram rate limit ceiling exceeded");
    }

    @Test
    @DisplayName("PT-FANOUT-02: HTTP 403 Forbidden (bot blocked by user) updates subscriber status to BLOCKED in DynamoDB")
    void shouldMarkSubscriberBlockedOnHttp403Forbidden() {
        // 1. Setup subscriber in replica repository
        SubscriptionChangedPayload subPayload = new SubscriptionChangedPayload(
                "sub-tok-blocked-user",
                "patna-ward12",
                Channel.TELEGRAM,
                "chat-blocked-user",
                Language.hi,
                Severity.WATCH,
                "CREATED"
        );
        subscriberReplicaService.replicateSubscription(subPayload);

        Optional<SubscriberReplica> initialSub = subscriberReplicaService.getSubscriber("patna-ward12", "sub-tok-blocked-user");
        assertThat(initialSub).isPresent();
        assertThat(initialSub.get().getStatus()).isEqualTo("ACTIVE");

        // 2. Setup mock RestClient returning HTTP 403
        RestClient.Builder clientBuilder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(clientBuilder).build();

        mockServer.expect(requestTo("https://api.telegram.org/botmock-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}"));

        TelegramBotClient customClient = new TelegramBotClient("mock-token", "chat-blocked-user", "https://api.telegram.org", clientBuilder.build());
        TelegramDeliveryQueueConsumer customConsumer = new TelegramDeliveryQueueConsumer(
                objectMapper,
                customClient,
                telegramRateLimiter,
                subscriberReplicaService,
                deliveryLogRepository
        );

        // 3. Dispatch delivery
        TelegramDeliveryPayload delivPayload = new TelegramDeliveryPayload(
                "deliv-403-test",
                "alert-blocked-test",
                "patna-ward12",
                "Ward 12, Patna",
                Severity.WARNING,
                "chat-blocked-user",
                "sub-tok-blocked-user",
                Language.hi,
                "बाढ़ चेतावनी",
                Provenance.MODEL_ONLY,
                null,
                Instant.now()
        );

        DeliveryLog logResult = customConsumer.deliver(delivPayload);
        mockServer.verify();

        assertThat(logResult.getStatus()).isEqualTo("BLOCKED");

        // 4. Verify subscriber replica status transitioned from ACTIVE -> BLOCKED in DynamoDB
        Optional<SubscriberReplica> updatedSub = subscriberReplicaService.getSubscriber("patna-ward12", "sub-tok-blocked-user");
        assertThat(updatedSub).isPresent();
        assertThat(updatedSub.get().getStatus()).isEqualTo("BLOCKED");
    }

    @Test
    @DisplayName("PT-FANOUT-02: AlertFanoutQueueFunction processes SQS batch from kestrel-delivery-telegram")
    void shouldProcessSqsBatchViaLambdaFunction() throws Exception {
        TelegramDeliveryPayload payload = new TelegramDeliveryPayload(
                "deliv-sqs-lambda",
                "alert-guwahati-99",
                "guwahati-river",
                "Riverside, Guwahati",
                Severity.SEVERE,
                "777888999",
                "tok-guwahati",
                Language.en,
                "Urgent: Brahmaputra discharge crossing 20-year return threshold",
                Provenance.MODEL_ONLY,
                null,
                Instant.now()
        );

        String messageBody = objectMapper.writeValueAsString(payload);
        Map<String, Object> sqsRecord = Map.of(
                "eventSourceARN", "arn:aws:sqs:ap-south-1:123456789012:kestrel-delivery-telegram-prod",
                "body", messageBody
        );
        Map<String, Object> lambdaEvent = Map.of("Records", List.of(sqsRecord));

        var function = alertFanoutQueueFunction.fanoutQueueFunction(null, null, telegramDeliveryConsumer, null);
        Map<String, Object> result = function.apply(lambdaEvent);

        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("telegramProcessed")).isEqualTo(1);

        List<DeliveryLog> receipts = deliveryLogRepository.findByAlertId("alert-guwahati-99");
        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).getRecipient()).isEqualTo("777888999");
    }

    @Test
    @DisplayName("GET /api/v1/fanout/rate-limits/telegram returns current rate limit statistics")
    void shouldReturnRateLimitStatusOverHttp() throws Exception {
        mockMvc.perform(get("/api/v1/fanout/rate-limits/telegram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetLimit").value(25))
                .andExpect(jsonPath("$.ceilingLimit").value(30))
                .andExpect(jsonPath("$.allowed").value(true));
    }
}
