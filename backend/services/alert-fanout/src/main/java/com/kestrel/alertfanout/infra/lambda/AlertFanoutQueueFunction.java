package com.kestrel.alertfanout.infra.lambda;

import com.kestrel.alertfanout.consumer.FanoutAlertsQueueConsumer;
import com.kestrel.alertfanout.consumer.FanoutRegistryQueueConsumer;
import com.kestrel.alertfanout.consumer.TelegramDeliveryQueueConsumer;
import com.kestrel.alertfanout.consumer.WebPushDeliveryQueueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * AWS Lambda Function Invoker bean for SQS events triggering AlertFanout Lambda.
 * Handles kestrel-fanout-alerts, kestrel-fanout-registry, kestrel-delivery-telegram,
 * and kestrel-delivery-webpush queue event batches.
 */
@Configuration
public class AlertFanoutQueueFunction {

    private static final Logger log = LoggerFactory.getLogger(AlertFanoutQueueFunction.class);

    @Bean(name = {"fanoutQueueFunction", "alertsQueueTrigger", "registryQueueTrigger", "telegramQueueTrigger", "webpushQueueTrigger"})
    public Function<Map<String, Object>, Map<String, Object>> fanoutQueueFunction(
            FanoutAlertsQueueConsumer alertsConsumer,
            FanoutRegistryQueueConsumer registryConsumer,
            TelegramDeliveryQueueConsumer telegramConsumer,
            WebPushDeliveryQueueConsumer webPushConsumer) {
        return event -> {
            if (event == null || event.isEmpty()) {
                log.warn("Received empty or null SQS event in AlertFanoutQueueFunction");
                return Map.of("status", "IGNORED", "reason", "empty event");
            }

            int processedAlerts = 0;
            int processedRegistry = 0;
            int processedTelegram = 0;
            int processedWebPush = 0;

            if (event.containsKey("Records") && event.get("Records") instanceof List<?> records) {
                log.info("Processing {} SQS records from event batch", records.size());
                for (Object recObj : records) {
                    if (recObj instanceof Map<?, ?> recordMap) {
                        String eventSourceArn = String.valueOf(recordMap.get("eventSourceARN"));
                        Object bodyObj = recordMap.get("body");
                        if (bodyObj instanceof String bodyStr) {
                            if (eventSourceArn != null && eventSourceArn.contains("fanout-registry")) {
                                registryConsumer.processMessage(bodyStr);
                                processedRegistry++;
                            } else if (eventSourceArn != null && eventSourceArn.contains("delivery-telegram")) {
                                telegramConsumer.processMessage(bodyStr);
                                processedTelegram++;
                            } else if (eventSourceArn != null && eventSourceArn.contains("delivery-webpush")) {
                                webPushConsumer.processMessage(bodyStr);
                                processedWebPush++;
                            } else if (eventSourceArn != null && eventSourceArn.contains("fanout-alerts")) {
                                alertsConsumer.processMessage(bodyStr);
                                processedAlerts++;
                            } else {
                                // Inspect body content if ARN is not indicative
                                if (bodyStr.contains("SubscriptionChanged") || bodyStr.contains("LocationUpserted")) {
                                    registryConsumer.processMessage(bodyStr);
                                    processedRegistry++;
                                } else if (bodyStr.contains("TelegramDelivery") || (bodyStr.contains("chatId") && bodyStr.contains("deliveryId"))) {
                                    telegramConsumer.processMessage(bodyStr);
                                    processedTelegram++;
                                } else if (bodyStr.contains("WebPushDelivery") || (bodyStr.contains("endpoint") && bodyStr.contains("deliveryId"))) {
                                    webPushConsumer.processMessage(bodyStr);
                                    processedWebPush++;
                                } else {
                                    alertsConsumer.processMessage(bodyStr);
                                    processedAlerts++;
                                }
                            }
                        }
                    }
                }
            } else if (event.containsKey("body") && event.get("body") instanceof String bodyStr) {
                if (bodyStr.contains("SubscriptionChanged") || bodyStr.contains("LocationUpserted")) {
                    registryConsumer.processMessage(bodyStr);
                    processedRegistry++;
                } else if (bodyStr.contains("TelegramDelivery") || (bodyStr.contains("chatId") && bodyStr.contains("deliveryId"))) {
                    telegramConsumer.processMessage(bodyStr);
                    processedTelegram++;
                } else if (bodyStr.contains("WebPushDelivery") || (bodyStr.contains("endpoint") && bodyStr.contains("deliveryId"))) {
                    webPushConsumer.processMessage(bodyStr);
                    processedWebPush++;
                } else {
                    alertsConsumer.processMessage(bodyStr);
                    processedAlerts++;
                }
            } else {
                log.warn("Unrecognized SQS event format in AlertFanoutQueueFunction: keys={}", event.keySet());
            }

            log.info("AlertFanout Lambda processed {} alerts, {} registry, {} telegram, {} webpush",
                    processedAlerts, processedRegistry, processedTelegram, processedWebPush);
            return Map.of(
                    "status", "SUCCESS",
                    "alertsProcessed", processedAlerts,
                    "registryProcessed", processedRegistry,
                    "telegramProcessed", processedTelegram,
                    "webPushProcessed", processedWebPush
            );
        };
    }
}
