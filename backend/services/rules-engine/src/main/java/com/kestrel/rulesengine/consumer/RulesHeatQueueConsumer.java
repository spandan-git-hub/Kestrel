package com.kestrel.rulesengine.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.HeatSignalPayload;
import com.kestrel.rulesengine.app.RulesEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumer for SQS queue kestrel-rules-heat.
 * Consumes real HeatSignal events published to SNS topic kestrel-signals.
 */
@Component
public class RulesHeatQueueConsumer extends SqsMessageHandler<HeatSignalPayload> {

    private static final Logger log = LoggerFactory.getLogger(RulesHeatQueueConsumer.class);

    private final RulesEvaluationService rulesEvaluationService;

    public RulesHeatQueueConsumer(ObjectMapper objectMapper, RulesEvaluationService rulesEvaluationService) {
        super(objectMapper, HeatSignalPayload.class);
        this.rulesEvaluationService = rulesEvaluationService;
    }

    @Override
    public void handle(EventEnvelope<HeatSignalPayload> envelope) {
        log.info("Consuming HeatSignal event from SQS: eventId={}, location={}",
                envelope.eventId(), envelope.payload().locationId());
        rulesEvaluationService.processHeatSignal(envelope.payload());
    }
}

