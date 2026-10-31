package com.kestrel.rulesengine.infra.lambda;

import com.kestrel.common.core.consumer.SqsMessageHandler;
import com.kestrel.rulesengine.consumer.RulesFloodQueueConsumer;
import com.kestrel.rulesengine.consumer.RulesHeatQueueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * AWS Lambda Function Invoker beans for SQS queues:
 * - kestrel-rules-flood (consumed by rulesFloodQueueFunction)
 * - kestrel-rules-heat  (consumed by rulesHeatQueueFunction)
 * Providing complete bulkhead isolation across hazards.
 */
@Configuration
public class RulesEngineQueueFunction {

    private static final Logger log = LoggerFactory.getLogger(RulesEngineQueueFunction.class);

    @Bean(name = {"rulesFloodQueueFunction", "rulesEngineQueueTrigger"})
    public Function<Map<String, Object>, Map<String, Object>> rulesFloodQueueFunction(
            RulesFloodQueueConsumer consumer) {
        return event -> processSqsRecords(event, consumer, "rulesFloodQueueFunction");
    }

    @Bean(name = "rulesHeatQueueFunction")
    public Function<Map<String, Object>, Map<String, Object>> rulesHeatQueueFunction(
            RulesHeatQueueConsumer consumer) {
        return event -> processSqsRecords(event, consumer, "rulesHeatQueueFunction");
    }

    private Map<String, Object> processSqsRecords(
            Map<String, Object> event,
            SqsMessageHandler<?> consumer,
            String functionName) {
        if (event == null || event.isEmpty()) {
            log.warn("Received empty or null SQS event in {}", functionName);
            return Map.of("status", "IGNORED", "reason", "empty event");
        }

        int processed = 0;
        if (event.containsKey("Records") && event.get("Records") instanceof List<?> records) {
            log.info("{} processing {} SQS records from event batch", functionName, records.size());
            for (Object recObj : records) {
                if (recObj instanceof Map<?, ?> recordMap) {
                    Object body = recordMap.get("body");
                    if (body instanceof String bodyStr) {
                        consumer.processMessage(bodyStr);
                        processed++;
                    }
                }
            }
        } else if (event.containsKey("body") && event.get("body") instanceof String bodyStr) {
            consumer.processMessage(bodyStr);
            processed++;
        } else {
            log.warn("Unrecognized SQS event format in {}: keys={}", functionName, event.keySet());
        }

        log.info("{} successfully processed {} SQS records", functionName, processed);
        return Map.of(
                "status", "SUCCESS",
                "recordsProcessed", processed
        );
    }
}
