package com.kestrel.subscriberregistry.infra.publisher;

import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.SubscriptionChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SubscriptionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public SubscriptionEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
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
    }
}
