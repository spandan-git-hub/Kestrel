package com.kestrel.alertfanout.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.app.LocationReplicaService;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.LocationUpsertedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FanoutRegistryQueueConsumer extends SqsMessageHandler<LocationUpsertedPayload> {

    private static final Logger log = LoggerFactory.getLogger(FanoutRegistryQueueConsumer.class);

    private final LocationReplicaService locationReplicaService;

    public FanoutRegistryQueueConsumer(ObjectMapper objectMapper, LocationReplicaService locationReplicaService) {
        super(objectMapper, LocationUpsertedPayload.class);
        this.locationReplicaService = locationReplicaService;
    }

    @Override
    public void handle(EventEnvelope<LocationUpsertedPayload> envelope) {
        log.info("Consuming LocationUpserted event from SQS: eventId={}, location={}",
                envelope.eventId(), envelope.payload().locationId());
        locationReplicaService.upsertLocationReplica(envelope.payload());
    }
}
