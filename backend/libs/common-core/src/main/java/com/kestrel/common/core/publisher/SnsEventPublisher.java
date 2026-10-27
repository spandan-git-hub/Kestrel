package com.kestrel.common.core.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

/**
 * Publishes standardized EventEnvelope<T> to AWS SNS topics with x-event-type message attributes.
 */
@Component
public class SnsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SnsEventPublisher.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    public SnsEventPublisher(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    public <T> String publish(String topicArn, EventEnvelope<T> envelope) {
        try {
            String messageJson = objectMapper.writeValueAsString(envelope);
            String eventType = envelope.eventType();

            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messageJson)
                    .messageAttributes(Map.of(
                            "x-event-type", MessageAttributeValue.builder()
                                     .dataType("String")
                                     .stringValue(eventType)
                                     .build()
                    ))
                    .build();

            PublishResponse response = snsClient.publish(request);
            log.info("Published event {} (type={}) to SNS topic {} with messageId={}",
                    envelope.eventId(), eventType, topicArn, response.messageId());
            return response.messageId();
        } catch (Exception e) {
            log.error("Failed to publish event {} to SNS topic {}", envelope.eventId(), topicArn, e);
            throw new RuntimeException("Failed to publish SNS event: " + envelope.eventId(), e);
        }
    }
}
