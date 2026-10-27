package com.kestrel.rulesengine.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.rulesengine.app.RulesEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RulesFloodQueueConsumer extends SqsMessageHandler<FloodSignalPayload> {

    private static final Logger log = LoggerFactory.getLogger(RulesFloodQueueConsumer.class);

    private final RulesEvaluationService rulesEvaluationService;

    public RulesFloodQueueConsumer(ObjectMapper objectMapper, RulesEvaluationService rulesEvaluationService) {
        super(objectMapper, FloodSignalPayload.class);
        this.rulesEvaluationService = rulesEvaluationService;
    }

    @Override
    public void handle(EventEnvelope<FloodSignalPayload> envelope) {
        log.info("Consuming FloodSignal event from SQS: eventId={}, location={}",
                envelope.eventId(), envelope.payload().locationId());
        rulesEvaluationService.processFloodSignal(envelope.payload());
    }
}
