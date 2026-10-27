package com.kestrel.alertfanout.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.app.AlertProcessingService;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.AlertEventPayload;
import com.kestrel.common.events.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FanoutAlertsQueueConsumer extends SqsMessageHandler<AlertEventPayload> {

    private static final Logger log = LoggerFactory.getLogger(FanoutAlertsQueueConsumer.class);

    private final AlertProcessingService alertProcessingService;

    public FanoutAlertsQueueConsumer(ObjectMapper objectMapper, AlertProcessingService alertProcessingService) {
        super(objectMapper, AlertEventPayload.class);
        this.alertProcessingService = alertProcessingService;
    }

    @Override
    public void handle(EventEnvelope<AlertEventPayload> envelope) {
        log.info("Consuming AlertEvent from SQS: eventId={}, alertId={}, location={}",
                envelope.eventId(), envelope.payload().alertId(), envelope.payload().locationId());
        alertProcessingService.processAlertEvent(envelope.payload());
    }
}
