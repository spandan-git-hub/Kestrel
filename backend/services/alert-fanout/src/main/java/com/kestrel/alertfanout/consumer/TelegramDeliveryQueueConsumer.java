package com.kestrel.alertfanout.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.alertfanout.app.SubscriberReplicaService;
import com.kestrel.alertfanout.infra.client.TelegramBotClient;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.limiter.TelegramRateLimiter;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.TelegramDeliveryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Real Telegram Bot API Worker consuming delivery tasks from SQS queue kestrel-delivery-telegram.
 * Enforces token-bucket rate limits (target 25 msg/s, max 30 msg/s ceiling), calls Telegram Bot API,
 * marks blocked subscribers in DynamoDB on HTTP 403, and records delivery receipts in kestrel_fanout_delivery.
 */
@Component
public class TelegramDeliveryQueueConsumer extends SqsMessageHandler<TelegramDeliveryPayload> {

    private static final Logger log = LoggerFactory.getLogger(TelegramDeliveryQueueConsumer.class);

    private final TelegramBotClient telegramBotClient;
    private final TelegramRateLimiter rateLimiter;
    private final SubscriberReplicaService subscriberReplicaService;
    private final DeliveryLogRepository deliveryLogRepository;
    private final long maxWaitMs;

    @org.springframework.beans.factory.annotation.Autowired
    public TelegramDeliveryQueueConsumer(
            ObjectMapper objectMapper,
            TelegramBotClient telegramBotClient,
            TelegramRateLimiter rateLimiter,
            SubscriberReplicaService subscriberReplicaService,
            DeliveryLogRepository deliveryLogRepository,
            @org.springframework.beans.factory.annotation.Value("${kestrel.telegram.rate-limit.max-wait-ms:200}") long maxWaitMs) {
        super(objectMapper, TelegramDeliveryPayload.class);
        this.telegramBotClient = telegramBotClient;
        this.rateLimiter = rateLimiter;
        this.subscriberReplicaService = subscriberReplicaService;
        this.deliveryLogRepository = deliveryLogRepository;
        this.maxWaitMs = maxWaitMs;
    }

    public TelegramDeliveryQueueConsumer(
            ObjectMapper objectMapper,
            TelegramBotClient telegramBotClient,
            TelegramRateLimiter rateLimiter,
            SubscriberReplicaService subscriberReplicaService,
            DeliveryLogRepository deliveryLogRepository) {
        this(objectMapper, telegramBotClient, rateLimiter, subscriberReplicaService, deliveryLogRepository, 200L);
    }

    @Override
    public void processMessage(String messageBody) {
        try {
            JsonNode root = objectMapper.readTree(messageBody);

            // Detect if wrapped by SNS notification JSON
            if (root.has("Type") && "Notification".equals(root.get("Type").asText()) && root.has("Message")) {
                String innerMessage = root.get("Message").asText();
                root = objectMapper.readTree(innerMessage);
            }

            TelegramDeliveryPayload payload;
            if (root.has("payload")) {
                payload = objectMapper.treeToValue(root.get("payload"), TelegramDeliveryPayload.class);
            } else {
                payload = objectMapper.treeToValue(root, TelegramDeliveryPayload.class);
            }

            log.info("Processing Telegram delivery SQS message: deliveryId={}, alertId={}, chat={}",
                    payload.deliveryId(), payload.alertId(), payload.chatId());
            deliver(payload);
        } catch (Exception e) {
            log.error("SQS processing error on Telegram delivery queue; propagating exception for DLQ redrive: {}",
                    e.getMessage(), e);
            throw new RuntimeException("Telegram delivery processing error, triggering DLQ redrive", e);
        }
    }

    @Override
    public void handle(EventEnvelope<TelegramDeliveryPayload> envelope) {
        if (envelope != null && envelope.payload() != null) {
            deliver(envelope.payload());
        }
    }

    /**
     * Executes the localized Telegram alert dispatch with rate-limiting and status tracking.
     */
    public DeliveryLog deliver(TelegramDeliveryPayload payload) {
        if (payload == null) {
            log.warn("Ignoring null Telegram delivery payload");
            return null;
        }

        // 1. Enforce Token-Bucket Rate Limiting (25 msg/s target, max 30 msg/s ceiling)
        boolean permitAcquired = rateLimiter.acquireWithBackoff(maxWaitMs);
        if (!permitAcquired) {
            log.warn("Telegram rate limit ceiling exceeded for deliveryId={}, chatId={}",
                    payload.deliveryId(), payload.chatId());
            DeliveryLog rateLimitedLog = createDeliveryLog(payload, "RATE_LIMITED");
            deliveryLogRepository.save(rateLimitedLog);
            throw new IllegalStateException("Telegram rate limit ceiling exceeded (30 msg/s), triggering SQS retry");
        }

        // 2. Dispatch to Telegram Bot API
        log.info("Dispatching Telegram alert to chat={} for location={}", payload.chatId(), payload.locationId());
        TelegramBotClient.TelegramDeliveryResult result = telegramBotClient.sendMessage(payload.chatId(), payload.messageText());

        // 3. Handle HTTP 403 (User blocked bot) -> mark subscriber as BLOCKED in DynamoDB
        if ("BLOCKED".equalsIgnoreCase(result.status()) || result.statusCode() == 403) {
            log.warn("Telegram user blocked bot for chat={} (token={}). Updating replica status to BLOCKED.",
                    payload.chatId(), payload.subscriberToken());
            if (payload.subscriberToken() != null && !payload.subscriberToken().isBlank() && payload.locationId() != null) {
                subscriberReplicaService.updateSubscriberStatus(payload.locationId(), payload.subscriberToken(), "BLOCKED");
            }
        }

        // 4. Record delivery receipt in DynamoDB table kestrel_fanout_delivery
        DeliveryLog deliveryLog = createDeliveryLog(payload, result.status());
        DeliveryLog saved = deliveryLogRepository.save(deliveryLog);
        log.info("Recorded Telegram delivery receipt: deliveryId={}, status={}", saved.getDeliveryId(), saved.getStatus());
        return saved;
    }

    private DeliveryLog createDeliveryLog(TelegramDeliveryPayload payload, String status) {
        String recipient = payload.chatId();
        if (recipient == null || recipient.isBlank()) {
            recipient = telegramBotClient.isConfigured() ? "@kestrel_alert_channel" : "@kestrel_broadcast";
        }

        DeliveryLog deliveryLog = new DeliveryLog(
                payload.alertId(),
                payload.locationId(),
                payload.locationName(),
                payload.severity(),
                Channel.TELEGRAM,
                recipient,
                payload.language(),
                Instant.now(),
                truncate(payload.messageText(), 1000),
                payload.provenance(),
                status
        );
        if (payload.deliveryId() != null && !payload.deliveryId().isBlank()) {
            deliveryLog.setDeliveryId(payload.deliveryId());
        }
        return deliveryLog;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
