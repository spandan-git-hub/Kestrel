package com.kestrel.common.core.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kestrel.common.core.publisher.SnsEventPublisher;
import com.kestrel.common.events.EventEnvelope;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SnsEventPublisherTest {

    @Test
    void publish_sendsMessageWithEventTypeAttribute() {
        AtomicReference<PublishRequest> lastRequest = new AtomicReference<>();
        SnsClient fakeSnsClient = new SnsClient() {
            @Override
            public String serviceName() {
                return "sns";
            }

            @Override
            public void close() {}

            @Override
            public PublishResponse publish(PublishRequest publishRequest) {
                lastRequest.set(publishRequest);
                return PublishResponse.builder().messageId("msg-12345").build();
            }
        };

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SnsEventPublisher publisher = new SnsEventPublisher(fakeSnsClient, objectMapper);

        EventEnvelope<String> envelope = EventEnvelope.of(
                "TestEvent",
                "test-producer",
                "corr-999",
                "test-payload"
        );

        String topicArn = "arn:aws:sns:ap-south-1:123456789012:kestrel-alerts-test";
        String messageId = publisher.publish(topicArn, envelope);

        assertThat(messageId).isEqualTo("msg-12345");
        PublishRequest captured = lastRequest.get();
        assertThat(captured).isNotNull();
        assertThat(captured.topicArn()).isEqualTo(topicArn);
        assertThat(captured.messageAttributes()).containsKey("x-event-type");
        assertThat(captured.messageAttributes().get("x-event-type").stringValue()).isEqualTo("TestEvent");
        assertThat(captured.message()).contains("test-payload");
        assertThat(captured.message()).contains(envelope.eventId());
    }
}
