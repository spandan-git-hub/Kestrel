package com.kestrel.rulesengine.infra.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.FloodSignalPayload;
import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.HeatSignalPayload;
import com.kestrel.common.events.HeatSignalPayload.HeatDayStats;
import com.kestrel.common.events.RegionType;
import com.kestrel.rulesengine.consumer.RulesFloodQueueConsumer;
import com.kestrel.rulesengine.consumer.RulesHeatQueueConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RulesEngineQueueFunctionTest {

    @Autowired
    private RulesEngineQueueFunction queueFunctionConfig;

    @Autowired
    private RulesFloodQueueConsumer queueConsumer;

    @Autowired
    private RulesHeatQueueConsumer heatQueueConsumer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Lambda SQS event with Records correctly invokes consumer and processes FloodSignal")
    void sqsEventWithRecordsProcessesSuccessfully() throws Exception {
        LocalDate today = LocalDate.now();
        List<ForecastDayStats> days = new ArrayList<>();
        days.add(new ForecastDayStats(today, 15, 20, 30, 40, 50, 0.45, 0.10, 0.0));
        for (int i = 1; i < 10; i++) {
            days.add(new ForecastDayStats(today.plusDays(i), 15, 20, 30, 40, 50, 0.1, 0.0, 0.0));
        }

        FloodSignalPayload payload = new FloodSignalPayload(
                "ganga-varanasi",
                Instant.now(),
                false,
                51,
                days
        );

        EventEnvelope<FloodSignalPayload> envelope = EventEnvelope.of(
                "FloodSignal",
                "flood-ingest",
                "lambda-sqs-test",
                payload
        );

        String envelopeJson = objectMapper.writeValueAsString(envelope);

        // Wrap as simulated AWS SQS event record
        Map<String, Object> record = Map.of(
                "messageId", "msg-sqs-12345",
                "body", envelopeJson
        );

        Map<String, Object> sqsEvent = Map.of(
                "Records", List.of(record)
        );

        var function = queueFunctionConfig.rulesFloodQueueFunction(queueConsumer);
        Map<String, Object> result = function.apply(sqsEvent);

        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("recordsProcessed")).isEqualTo(1);
    }

    @Test
    @DisplayName("Lambda SQS event with SNS notification wrapper unwraps and processes payload")
    void snsWrappedSqsEventUnwrapsAndProcesses() throws Exception {
        LocalDate today = LocalDate.now();
        List<ForecastDayStats> days = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            days.add(new ForecastDayStats(today.plusDays(i), 15, 20, 30, 40, 50, 0.05, 0.0, 0.0));
        }

        FloodSignalPayload payload = new FloodSignalPayload(
                "brahmaputra-dibrugarh",
                Instant.now(),
                false,
                50,
                days
        );

        EventEnvelope<FloodSignalPayload> envelope = EventEnvelope.of(
                "FloodSignal",
                "flood-ingest",
                "lambda-sns-sqs-test",
                payload
        );

        String innerMessageJson = objectMapper.writeValueAsString(envelope);

        // Simulate SNS notification JSON structure that SQS receives when subscribed to SNS
        Map<String, Object> snsNotification = Map.of(
                "Type", "Notification",
                "MessageId", "sns-msg-999",
                "TopicArn", "arn:aws:sns:ap-south-1:123456789012:kestrel-signals-prod",
                "Message", innerMessageJson
        );

        String snsMessageBody = objectMapper.writeValueAsString(snsNotification);

        Map<String, Object> record = Map.of(
                "messageId", "msg-sqs-sns-5678",
                "body", snsMessageBody
        );

        Map<String, Object> sqsEvent = Map.of(
                "Records", List.of(record)
        );

        var function = queueFunctionConfig.rulesFloodQueueFunction(queueConsumer);
        Map<String, Object> result = function.apply(sqsEvent);

        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("recordsProcessed")).isEqualTo(1);
    }

    @Test
    @DisplayName("Lambda SQS event with Records correctly invokes rulesHeatQueueFunction for HeatSignal")
    void heatSqsEventProcessesSuccessfully() throws Exception {
        LocalDate today = LocalDate.now();
        List<HeatDayStats> days = new ArrayList<>();
        days.add(new HeatDayStats(today, 43.0, 46.0, 35.0, 37.0, 6.0));
        days.add(new HeatDayStats(today.plusDays(1), 44.0, 47.0, 35.0, 37.0, 7.0));
        for (int i = 2; i < 7; i++) {
            days.add(new HeatDayStats(today.plusDays(i), 38.0, 40.0, 40.0, 37.0, 1.0));
        }

        HeatSignalPayload payload = new HeatSignalPayload(
                "yamuna-delhi",
                "run-heat-sqs-test",
                Instant.now(),
                Instant.now(),
                RegionType.PLAINS,
                days
        );

        EventEnvelope<HeatSignalPayload> envelope = EventEnvelope.of(
                "HeatSignal",
                "heat-ingest",
                "lambda-sqs-heat-test",
                payload
        );

        String envelopeJson = objectMapper.writeValueAsString(envelope);

        Map<String, Object> record = Map.of(
                "messageId", "msg-sqs-heat-12345",
                "body", envelopeJson
        );

        Map<String, Object> sqsEvent = Map.of(
                "Records", List.of(record)
        );

        var function = queueFunctionConfig.rulesHeatQueueFunction(heatQueueConsumer);
        Map<String, Object> result = function.apply(sqsEvent);

        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("recordsProcessed")).isEqualTo(1);
    }
}

