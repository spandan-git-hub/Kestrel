package com.kestrel.rulesengine.infra.lambda;

import com.kestrel.rulesengine.consumer.RulesFloodQueueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * AWS Lambda Function Invoker bean for SQS queue kestrel-rules-flood events.
 * Consumes real FloodSignal events published to SNS topic kestrel-signals and redriven to SQS.
 */
@Configuration
public class RulesEngineQueueFunction {

    private static final Logger log = LoggerFactory.getLogger(RulesEngineQueueFunction.class);

    @Bean(name = {"rulesFloodQueueFunction", "rulesEngineQueueTrigger"})
    public Function<Map<String, Object>, Map<String, Object>> rulesFloodQueueFunction(
            RulesFloodQueueConsumer consumer) {
        return event -> {
            if (event == null || event.isEmpty()) {
                log.warn("Received empty or null SQS event in RulesEngineQueueFunction");
                return Map.of("status", "IGNORED", "reason", "empty event");
            }

            int processed = 0;
            if (event.containsKey("Records") && event.get("Records") instanceof List<?> records) {
                log.info("Processing {} SQS records from event batch", records.size());
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
                log.warn("Unrecognized SQS event format in RulesEngineQueueFunction: keys={}", event.keySet());
            }

            log.info("RulesEngine Lambda successfully processed {} SQS records", processed);
            return Map.of(
                    "status", "SUCCESS",
                    "recordsProcessed", processed
            );
        };
    }
}

