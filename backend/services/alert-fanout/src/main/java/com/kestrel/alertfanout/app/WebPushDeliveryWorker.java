package com.kestrel.alertfanout.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.consumer.WebPushDeliveryQueueConsumer;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.common.events.WebPushDeliveryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Orchestrator service for Web Push browser deliveries.
 * When SQS delivery queue URL is configured, enqueues to kestrel-delivery-webpush.
 * Otherwise, falls back to direct synchronous dispatch.
 */
@Service
public class WebPushDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(WebPushDeliveryWorker.class);

    private final SqsClient sqsClient;
    private final String webPushQueueUrl;
    private final ObjectMapper objectMapper;
    private final WebPushDeliveryQueueConsumer consumer;

    @Autowired
    public WebPushDeliveryWorker(
            SqsClient sqsClient,
            @Value("${sqs.delivery-webpush.url:${SQS_DELIVERY_WEBPUSH_URL:}}") String webPushQueueUrl,
            ObjectMapper objectMapper,
            WebPushDeliveryQueueConsumer consumer) {
        this.sqsClient = sqsClient;
        this.webPushQueueUrl = webPushQueueUrl;
        this.objectMapper = objectMapper;
        this.consumer = consumer;
    }

    public DeliveryLog dispatch(WebPushDeliveryPayload payload) {
        if (payload == null) {
            log.warn("Cannot dispatch null Web Push delivery payload");
            return null;
        }

        if (webPushQueueUrl != null && !webPushQueueUrl.isBlank()) {
            try {
                String body = objectMapper.writeValueAsString(payload);
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(webPushQueueUrl)
                        .messageBody(body)
                        .build());
                log.info("Enqueued Web Push delivery message to SQS queue {}: deliveryId={}",
                        webPushQueueUrl, payload.deliveryId());
                return null;
            } catch (Exception e) {
                log.warn("Failed to publish Web Push delivery to SQS queue {}: {}. Falling back to direct worker dispatch.",
                        webPushQueueUrl, e.getMessage());
            }
        }

        // Direct execution
        return consumer.deliver(payload);
    }
}

