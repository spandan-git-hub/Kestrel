package com.kestrel.alertfanout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.api.dto.BroadcastRequest;
import com.kestrel.alertfanout.app.SubscriberReplicaService;
import com.kestrel.alertfanout.consumer.WebPushDeliveryQueueConsumer;
import com.kestrel.alertfanout.infra.client.WebPushClient;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.alertfanout.infra.lambda.AlertFanoutQueueFunction;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.alertfanout.infra.repository.SubscriberReplicaRepository;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import com.kestrel.common.events.SubscriptionChangedPayload;
import com.kestrel.common.events.WebPushDeliveryPayload;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebPushWorkerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebPushDeliveryQueueConsumer webPushConsumer;

    @Autowired
    private SubscriberReplicaService subscriberReplicaService;

    @Autowired
    private SubscriberReplicaRepository subscriberReplicaRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private AlertFanoutQueueFunction alertFanoutQueueFunction;

    @BeforeEach
    void setUp() {
        deliveryLogRepository.deleteAll();
        subscriberReplicaRepository.deleteAll();
    }

    @Test
    @DisplayName("PT-FANOUT-03: Consumes Web Push delivery payload, delivers browser push, and records receipt")
    void shouldProcessWebPushDeliveryAndRecordReceipt() {
        WebPushDeliveryPayload payload = new WebPushDeliveryPayload(
                "deliv-wp-001",
                "alert-mumbai-300",
                "mumbai-kurla",
                "Kurla Ward L, Mumbai",
                Severity.WARNING,
                "https://fcm.googleapis.com/fcm/send/test-device-token",
                "p256dh-key",
                "auth-key",
                "sub-tok-kurla-wp",
                Language.mr,
                "पूर इशारा - कुर्ला",
                "मिठी नदीच्या पाणी पातळीत वाढ होत आहे. सतर्क रहा.",
                "/dashboard?loc=mumbai-kurla",
                Provenance.MODEL_ONLY,
                Instant.now()
        );

        DeliveryLog result = webPushConsumer.deliver(payload);

        assertThat(result).isNotNull();
        assertThat(result.getDeliveryId()).isEqualTo("deliv-wp-001");
        assertThat(result.getAlertId()).isEqualTo("alert-mumbai-300");
        assertThat(result.getLocationId()).isEqualTo("mumbai-kurla");
        assertThat(result.getChannel()).isEqualTo(Channel.WEB_PUSH);
        assertThat(result.getRecipient()).isEqualTo("https://fcm.googleapis.com/fcm/send/test-device-token");
        assertThat(result.getLanguage()).isEqualTo(Language.mr);
        assertThat(result.getStatus()).isEqualTo("SIMULATED"); // default no VAPID keys in test
        assertThat(result.getMessageExcerpt()).contains("पूर इशारा");

        List<DeliveryLog> receipts = deliveryLogRepository.findByAlertId("alert-mumbai-300");
        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).getChannel()).isEqualTo(Channel.WEB_PUSH);
    }

    @Test
    @DisplayName("PT-FANOUT-03: Real VAPID push dispatch to browser endpoint with HTTP 201 Created")
    void shouldDispatchRealVapidPushNotification() {
        String pushEndpoint = "https://updates.push.services.mozilla.com/wpush/v2/test-endpoint";
        RestClient.Builder clientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(clientBuilder).build();

        mockServer.expect(requestTo(pushEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("TTL", "86400"))
                .andExpect(header("Urgency", "high"))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("vapid ")))
                .andRespond(withStatus(HttpStatus.CREATED));

        WebPushClient customClient = new WebPushClient("mock-public-key", "mock-private-key", "mailto:ops@kestrel.in", clientBuilder.build());
        WebPushDeliveryQueueConsumer customConsumer = new WebPushDeliveryQueueConsumer(
                objectMapper,
                customClient,
                subscriberReplicaService,
                deliveryLogRepository
        );

        WebPushDeliveryPayload payload = new WebPushDeliveryPayload(
                "deliv-real-vapid",
                "alert-real-vapid-1",
                "patna-ward12",
                "Ward 12, Patna",
                Severity.SEVERE,
                pushEndpoint,
                "p256dh",
                "auth",
                "tok-patna-wp",
                Language.hi,
                "बाढ़ चेतावनी",
                "गंगा नदी खतरे के निशान से ऊपर",
                "/dashboard?loc=patna-ward12",
                Provenance.MODEL_ONLY,
                Instant.now()
        );

        DeliveryLog receipt = customConsumer.deliver(payload);
        mockServer.verify();

        assertThat(receipt.getStatus()).isEqualTo("DELIVERED");
        assertThat(receipt.getChannel()).isEqualTo(Channel.WEB_PUSH);
    }

    @Test
    @DisplayName("PT-FANOUT-03: HTTP 410 Gone / 404 Not Found expired browser endpoint updates subscriber to BLOCKED")
    void shouldMarkSubscriberBlockedOnHttp410ExpiredEndpoint() {
        // 1. Setup subscriber in replica repository
        SubscriptionChangedPayload subPayload = new SubscriptionChangedPayload(
                "sub-tok-expired-browser",
                "cuttack-delta",
                Channel.WEB_PUSH,
                "https://fcm.googleapis.com/fcm/send/expired-token-123",
                Language.en,
                Severity.WARNING,
                "CREATED"
        );
        subscriberReplicaService.replicateSubscription(subPayload);

        Optional<SubscriberReplica> initialSub = subscriberReplicaService.getSubscriber("cuttack-delta", "sub-tok-expired-browser");
        assertThat(initialSub).isPresent();
        assertThat(initialSub.get().getStatus()).isEqualTo("ACTIVE");

        // 2. Setup mock server returning HTTP 410 Gone
        String pushEndpoint = "https://fcm.googleapis.com/fcm/send/expired-token-123";
        RestClient.Builder clientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(clientBuilder).build();

        mockServer.expect(requestTo(pushEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.GONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"subscription has expired or unsubscribed\"}"));

        WebPushClient customClient = new WebPushClient("mock-public-key", "mock-private-key", "mailto:ops@kestrel.in", clientBuilder.build());
        WebPushDeliveryQueueConsumer customConsumer = new WebPushDeliveryQueueConsumer(
                objectMapper,
                customClient,
                subscriberReplicaService,
                deliveryLogRepository
        );

        // 3. Dispatch Web Push
        WebPushDeliveryPayload payload = new WebPushDeliveryPayload(
                "deliv-expired-410",
                "alert-cuttack-900",
                "cuttack-delta",
                "Cuttack Municipal Corp",
                Severity.WARNING,
                pushEndpoint,
                "p256dh",
                "auth",
                "sub-tok-expired-browser",
                Language.en,
                "Flood Warning",
                "Mahanadi discharge elevated",
                "/dashboard?loc=cuttack-delta",
                Provenance.MODEL_ONLY,
                Instant.now()
        );

        DeliveryLog receipt = customConsumer.deliver(payload);
        mockServer.verify();

        assertThat(receipt.getStatus()).isEqualTo("EXPIRED");

        // 4. Verify subscriber replica in DynamoDB transitioned to BLOCKED
        Optional<SubscriberReplica> updatedSub = subscriberReplicaService.getSubscriber("cuttack-delta", "sub-tok-expired-browser");
        assertThat(updatedSub).isPresent();
        assertThat(updatedSub.get().getStatus()).isEqualTo("BLOCKED");
    }

    @Test
    @DisplayName("PT-FANOUT-03: AlertFanoutQueueFunction processes SQS batch from kestrel-delivery-webpush")
    void shouldProcessWebPushSqsBatchViaLambdaFunction() throws Exception {
        WebPushDeliveryPayload payload = new WebPushDeliveryPayload(
                "deliv-sqs-wp",
                "alert-guwahati-88",
                "guwahati-river",
                "Riverside, Guwahati",
                Severity.SEVERE,
                "https://updates.push.services.mozilla.com/wpush/v2/guwahati-resident",
                "p256dh",
                "auth",
                "tok-guwahati-wp",
                Language.en,
                "Emergency Evacuation Notice",
                "Brahmaputra peak discharge crossing danger level",
                "/dashboard?loc=guwahati-river",
                Provenance.MODEL_ONLY,
                Instant.now()
        );

        String messageBody = objectMapper.writeValueAsString(payload);
        Map<String, Object> sqsRecord = Map.of(
                "eventSourceARN", "arn:aws:sqs:ap-south-1:123456789012:kestrel-delivery-webpush-prod",
                "body", messageBody
        );
        Map<String, Object> lambdaEvent = Map.of("Records", List.of(sqsRecord));

        var function = alertFanoutQueueFunction.fanoutQueueFunction(null, null, null, webPushConsumer);
        Map<String, Object> result = function.apply(lambdaEvent);

        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("webPushProcessed")).isEqualTo(1);

        List<DeliveryLog> receipts = deliveryLogRepository.findByAlertId("alert-guwahati-88");
        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).getRecipient()).isEqualTo("https://updates.push.services.mozilla.com/wpush/v2/guwahati-resident");
    }

    @Test
    @DisplayName("PT-FANOUT-03: POST /api/v1/fanout/broadcast dispatches to Web Push subscribers")
    void shouldFanoutBroadcastToWebPushSubscribers() throws Exception {
        // Register Web Push subscriber for Patna
        SubscriptionChangedPayload subPayload = new SubscriptionChangedPayload(
                "tok-resident-patna-wp",
                "patna-ward12",
                Channel.WEB_PUSH,
                "https://fcm.googleapis.com/fcm/send/patna-device-1",
                Language.hi,
                Severity.WARNING,
                "CREATED"
        );
        subscriberReplicaService.replicateSubscription(subPayload);

        BroadcastRequest request = new BroadcastRequest(
                "patna-ward12",
                Hazard.FLOOD,
                Severity.SEVERE,
                "Surge water level rising near Digha Ghat.",
                List.of(Channel.WEB_PUSH)
        );

        mockMvc.perform(post("/api/v1/fanout/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broadcastId").exists())
                .andExpect(jsonPath("$.locationId").value("patna-ward12"))
                .andExpect(jsonPath("$.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.dispatchedCount").value(1));

        List<DeliveryLog> receipts = deliveryLogRepository.findByLocationIdOrderByTimestampDesc("patna-ward12");
        assertThat(receipts).isNotEmpty();
        assertThat(receipts.get(0).getChannel()).isEqualTo(Channel.WEB_PUSH);
    }
}

