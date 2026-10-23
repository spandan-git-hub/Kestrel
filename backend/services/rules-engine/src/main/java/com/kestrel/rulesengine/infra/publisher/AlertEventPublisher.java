package com.kestrel.rulesengine.infra.publisher;

import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AlertEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RestClient eventBusRestClient;

    @Value("${kestrel.events.fanout-url:http://localhost:8084/api/v1/fanout/events/alert}")
    private String fanoutEventUrl;

    public AlertEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            @Qualifier("eventBusRestClient") RestClient eventBusRestClient) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventBusRestClient = eventBusRestClient;
    }

    public void publish(AlertEventPayload payload) {
        String correlationId = UUID.randomUUID().toString();
        EventEnvelope<AlertEventPayload> envelope = EventEnvelope.of(
                "AlertEvent",
                "rules-engine",
                correlationId,
                payload
        );

        // 1. In-process Spring Event publishing
        applicationEventPublisher.publishEvent(envelope);
        log.info("Published AlertEvent locally: alertId={}, locationId={}, {} -> {}, autoSend={}",
                payload.alertId(), payload.locationId(), payload.fromSeverity(), payload.toSeverity(), payload.autoSend());

        // 2. Inter-service HTTP event delivery to alert-fanout
        try {
            eventBusRestClient.post()
                    .uri(fanoutEventUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Dispatched AlertEvent to alert-fanout at {}", fanoutEventUrl);
        } catch (Exception ex) {
            log.debug("alert-fanout not reachable at {} (normal during standalone test or startup): {}",
                    fanoutEventUrl, ex.getMessage());
        }
    }
}
