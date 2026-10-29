package com.kestrel.alertfanout.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.consumer.TelegramDeliveryQueueConsumer;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.common.events.TelegramDeliveryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Orchestrator service for Telegram alert deliveries.
 * When an SQS delivery queue URL is present, enqueues to kestrel-delivery-telegram.
 * Otherwise (e.g. in standalone / test runs), executes direct synchronous dispatch.
 */
@Service
public class TelegramDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(TelegramDeliveryWorker.class);

    private final SqsClient sqsClient;
    private final String telegramQueueUrl;
    private final ObjectMapper objectMapper;
    private final TelegramDeliveryQueueConsumer consumer;

    public TelegramDeliveryWorker(
            SqsClient sqsClient,
            @Value("${sqs.delivery-telegram.url:${SQS_DELIVERY_TELEGRAM_URL:}}") String telegramQueueUrl,
            ObjectMapper objectMapper,
            TelegramDeliveryQueueConsumer consumer) {
        this.sqsClient = sqsClient;
        this.telegramQueueUrl = telegramQueueUrl;
        this.objectMapper = objectMapper;
        this.consumer = consumer;
    }

    public DeliveryLog dispatch(TelegramDeliveryPayload payload) {
        if (payload == null) {
            log.warn("Cannot dispatch null Telegram delivery payload");
            return null;
        }

        if (telegramQueueUrl != null && !telegramQueueUrl.isBlank()) {
            try {
                String body = objectMapper.writeValueAsString(payload);
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(telegramQueueUrl)
                        .messageBody(body)
                        .build());
                log.info("Enqueued Telegram delivery message to SQS queue {}: deliveryId={}",
                        telegramQueueUrl, payload.deliveryId());
                return null;
            } catch (Exception e) {
                log.warn("Failed to publish Telegram delivery to SQS queue {}: {}. Falling back to direct worker dispatch.",
                        telegramQueueUrl, e.getMessage());
            }
        }

        // Direct worker execution
        return consumer.deliver(payload);
    }
}

