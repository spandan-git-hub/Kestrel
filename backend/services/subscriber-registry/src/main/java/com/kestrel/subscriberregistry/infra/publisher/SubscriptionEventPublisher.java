package com.kestrel.subscriberregistry.infra.publisher;

import com.kestrel.common.core.publisher.SnsEventPublisher;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.SubscriptionChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SubscriptionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SnsEventPublisher snsEventPublisher;

    @Value("${kestrel.sns.registry-topic-arn:${SNS_REGISTRY_TOPIC_ARN:}}")
    private String registryTopicArn;

    public SubscriptionEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            ObjectProvider<SnsEventPublisher> snsEventPublisherProvider) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.snsEventPublisher = snsEventPublisherProvider.getIfAvailable();
    }

    public void publish(SubscriptionChangedPayload payload) {
        String correlationId = UUID.randomUUID().toString();
        EventEnvelope<SubscriptionChangedPayload> envelope = EventEnvelope.of(
                "SubscriptionChanged",
                "subscriber-registry",
                correlationId,
                payload
        );

        applicationEventPublisher.publishEvent(envelope);
        log.info("Published SubscriptionChanged locally: token={}, locationId={}, action={}",
                payload.token(), payload.locationId(), payload.action());

        if (snsEventPublisher != null && registryTopicArn != null && !registryTopicArn.isBlank()) {
            try {
                snsEventPublisher.publish(registryTopicArn, envelope);
                log.info("Published SubscriptionChanged to SNS topic {}", registryTopicArn);
            } catch (Exception e) {
                log.warn("SNS publish failed for SubscriptionChanged: {}", e.getMessage());
            }
        }
    }
}
