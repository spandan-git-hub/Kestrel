package com.kestrel.subscriberregistry.infra.publisher;

import com.kestrel.common.core.publisher.SnsEventPublisher;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.SubscriptionChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class SubscriptionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RestClient eventBusRestClient;
    private final SnsEventPublisher snsEventPublisher;

    @Value("${kestrel.sns.registry-topic-arn:${SNS_REGISTRY_TOPIC_ARN:}}")
    private String registryTopicArn;

    @Value("${kestrel.events.fanout-subscription-url:http://localhost:8084/api/v1/fanout/events/subscription-changed}")
    private String fanoutSubscriptionUrl;

    public SubscriptionEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            @Qualifier("eventBusRestClient") RestClient eventBusRestClient,
            ObjectProvider<SnsEventPublisher> snsEventPublisherProvider) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventBusRestClient = eventBusRestClient;
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

        // Downstream HTTP event distribution to alert-fanout
        sendToEndpoint(fanoutSubscriptionUrl, envelope, "alert-fanout");
    }

    private void sendToEndpoint(String url, EventEnvelope<?> envelope, String targetName) {
        try {
            eventBusRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Dispatched SubscriptionChanged event to {} at {}", targetName, url);
        } catch (Exception ex) {
            log.debug("{} not reachable at {} (normal during standalone test or startup): {}",
                    targetName, url, ex.getMessage());
        }
    }
}
