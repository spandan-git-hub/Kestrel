package com.kestrel.subscriberregistry.infra.publisher;

import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.LocationUpsertedPayload;
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
public class LocationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocationEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RestClient eventBusRestClient;

    @Value("${kestrel.events.flood-ingest-url:http://localhost:8082/api/v1/flood/events/location-upserted}")
    private String floodIngestLocationUrl;

    @Value("${kestrel.events.rules-engine-url:http://localhost:8083/api/v1/rules/events/location-upserted}")
    private String rulesEngineLocationUrl;

    @Value("${kestrel.events.fanout-url:http://localhost:8084/api/v1/fanout/events/location-upserted}")
    private String fanoutLocationUrl;

    public LocationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            @Qualifier("eventBusRestClient") RestClient eventBusRestClient) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventBusRestClient = eventBusRestClient;
    }

    public void publish(LocationUpsertedPayload payload) {
        String correlationId = UUID.randomUUID().toString();
        EventEnvelope<LocationUpsertedPayload> envelope = EventEnvelope.of(
                "LocationUpserted",
                "subscriber-registry",
                correlationId,
                payload
        );

        // 1. In-process Spring Event publishing
        applicationEventPublisher.publishEvent(envelope);
        log.info("Published LocationUpserted locally: locationId={}, name={}", payload.locationId(), payload.name());

        // 2. Downstream HTTP event distribution
        sendToEndpoint(floodIngestLocationUrl, envelope, "flood-ingest");
        sendToEndpoint(rulesEngineLocationUrl, envelope, "rules-engine");
        sendToEndpoint(fanoutLocationUrl, envelope, "alert-fanout");
    }

    private void sendToEndpoint(String url, EventEnvelope<?> envelope, String targetName) {
        try {
            eventBusRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Dispatched LocationUpserted event to {} at {}", targetName, url);
        } catch (Exception ex) {
            log.debug("{} not reachable at {} (normal during standalone test or startup): {}",
                    targetName, url, ex.getMessage());
        }
    }
}
