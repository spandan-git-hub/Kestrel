package com.kestrel.common.core.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqsMessageHandlerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void processMessage_directEnvelope_invokesHandler() {
        AtomicReference<EventEnvelope<String>> received = new AtomicReference<>();
        SqsMessageHandler<String> handler = new SqsMessageHandler<>(objectMapper, String.class) {
            @Override
            public void handle(EventEnvelope<String> envelope) {
                received.set(envelope);
            }
        };

        String json = """
                {
                    "eventId": "evt-123",
                    "eventType": "FloodSignal",
                    "schemaVersion": 1,
                    "occurredAt": "2026-10-27T12:00:00Z",
                    "producer": "flood-ingest",
                    "correlationId": "corr-456",
                    "payload": "mumbai-kurla"
                }
                """;

        handler.processMessage(json);

        assertThat(received.get()).isNotNull();
        assertThat(received.get().eventId()).isEqualTo("evt-123");
        assertThat(received.get().eventType()).isEqualTo("FloodSignal");
        assertThat(received.get().payload()).isEqualTo("mumbai-kurla");
    }

    @Test
    void processMessage_snsNotificationWrapper_unwrapsAndInvokesHandler() throws Exception {
        AtomicReference<EventEnvelope<String>> received = new AtomicReference<>();
        SqsMessageHandler<String> handler = new SqsMessageHandler<>(objectMapper, String.class) {
            @Override
            public void handle(EventEnvelope<String> envelope) {
                received.set(envelope);
            }
        };

        String innerEnvelope = "{\"eventId\":\"evt-789\",\"eventType\":\"AlertTriggered\",\"payload\":\"patna-ward12\"}";
        String snsWrapper = String.format("""
                {
                    "Type": "Notification",
                    "MessageId": "sns-msg-1",
                    "TopicArn": "arn:aws:sns:ap-south-1:123:kestrel-alerts",
                    "Message": %s
                }
                """, objectMapper.writeValueAsString(innerEnvelope));

        handler.processMessage(snsWrapper);

        assertThat(received.get()).isNotNull();
        assertThat(received.get().eventId()).isEqualTo("evt-789");
        assertThat(received.get().eventType()).isEqualTo("AlertTriggered");
        assertThat(received.get().payload()).isEqualTo("patna-ward12");
    }

    @Test
    void processMessage_handlerThrows_propagatesExceptionForDlqRedrive() {
        SqsMessageHandler<String> handler = new SqsMessageHandler<>(objectMapper, String.class) {
            @Override
            public void handle(EventEnvelope<String> envelope) {
                throw new IllegalStateException("Simulated processing error");
            }
        };

        String json = "{\"eventId\":\"evt-fail\",\"eventType\":\"ErrorEvent\",\"payload\":\"data\"}";

        assertThatThrownBy(() -> handler.processMessage(json))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQS processing error, triggering redrive");
    }
}
