package com.kestrel.heatingest.infra.publisher;

import com.kestrel.common.core.publisher.SnsEventPublisher;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.HeatSignalPayload;
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
public class HeatSignalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(HeatSignalEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RestClient eventBusRestClient;
    private final SnsEventPublisher snsEventPublisher;

    @Value("${kestrel.sns.signals-topic-arn:${SNS_SIGNALS_TOPIC_ARN:}}")
    private String signalsTopicArn;

    @Value("${kestrel.events.rules-engine-url:http://localhost:8083/api/v1/rules/events/heat-signal}")
    private String rulesEngineEventUrl;

    public HeatSignalEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            @Qualifier("eventBusRestClient") RestClient eventBusRestClient,
            ObjectProvider<SnsEventPublisher> snsEventPublisherProvider) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.eventBusRestClient = eventBusRestClient;
        this.snsEventPublisher = snsEventPublisherProvider.getIfAvailable();
    }

    public void publish(HeatSignalPayload payload) {
        String correlationId = UUID.randomUUID().toString();
        EventEnvelope<HeatSignalPayload> envelope = EventEnvelope.of(
                "HeatSignal",
                "heat-ingest",
                correlationId,
                payload
        );

        // 1. In-process Spring Event publishing
        applicationEventPublisher.publishEvent(envelope);
        log.info("Published HeatSignal event locally for location={}", payload.locationId());

        // 2. AWS SNS Signals Topic publishing
        if (snsEventPublisher != null && signalsTopicArn != null && !signalsTopicArn.isBlank()) {
            try {
                snsEventPublisher.publish(signalsTopicArn, envelope);
                log.info("Published HeatSignal to SNS topic {}", signalsTopicArn);
            } catch (Exception e) {
                log.warn("SNS publish failed for HeatSignal: {}", e.getMessage());
            }
        }

        // 3. Inter-service HTTP event delivery to rules-engine
        try {
            eventBusRestClient.post()
                    .uri(rulesEngineEventUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Dispatched HeatSignal event to rules-engine at {}", rulesEngineEventUrl);
        } catch (Exception ex) {
            log.debug("rules-engine not reachable at {} (normal during single-service test or startup): {}",
                    rulesEngineEventUrl, ex.getMessage());
        }
    }
}

