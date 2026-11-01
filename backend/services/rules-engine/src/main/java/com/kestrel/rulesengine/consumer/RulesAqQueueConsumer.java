package com.kestrel.rulesengine.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.AqSignalPayload;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.rulesengine.app.RulesEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumer for SQS queue kestrel-rules-aq.
 * Consumes real AqSignal events published to SNS topic kestrel-signals.
 */
@Component
public class RulesAqQueueConsumer extends SqsMessageHandler<AqSignalPayload> {

    private static final Logger log = LoggerFactory.getLogger(RulesAqQueueConsumer.class);

    private final RulesEvaluationService rulesEvaluationService;

    public RulesAqQueueConsumer(ObjectMapper objectMapper, RulesEvaluationService rulesEvaluationService) {
        super(objectMapper, AqSignalPayload.class);
        this.rulesEvaluationService = rulesEvaluationService;
    }

    @Override
    public void handle(EventEnvelope<AqSignalPayload> envelope) {
        log.info("Consuming AqSignal event from SQS: eventId={}, location={}",
                envelope.eventId(), envelope.payload().locationId());
        rulesEvaluationService.processAqSignal(envelope.payload());
    }
}

