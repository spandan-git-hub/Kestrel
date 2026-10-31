package com.kestrel.common.core.budget;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component("coreDynamoDbCallBudgetTracker")
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbCallBudgetTracker implements CallBudgetTracker {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbCallBudgetTracker.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final int maxMinuteLimit;
    private final int maxDayLimit;

    public DynamoDbCallBudgetTracker(
            DynamoDbClient dynamoDbClient,
            DynamoDbTableNameResolver tableNameResolver,
            @Value("${kestrel.open-meteo.rate-limit.minute-max:500}") int maxMinuteLimit,
            @Value("${kestrel.open-meteo.rate-limit.day-max:8000}") int maxDayLimit) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableNameResolver.resolve("kestrel_flood_call_budget", "DYNAMODB_FLOOD_CALL_BUDGET");
        this.maxMinuteLimit = maxMinuteLimit;
        this.maxDayLimit = maxDayLimit;
    }

    @PostConstruct
    public void initTable() {
        try {
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder().attributeName("pk").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder().attributeName("pk").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            log.info("Initialized DynamoDB table {} for Open-Meteo call budget tracking", tableName);
        } catch (ResourceInUseException e) {
            log.debug("DynamoDB table {} already exists", tableName);
        } catch (Exception e) {
            log.warn("Could not auto-create DynamoDB table {}: {}", tableName, e.getMessage());
        }

        try {
            dynamoDbClient.updateTimeToLive(UpdateTimeToLiveRequest.builder()
                    .tableName(tableName)
                    .timeToLiveSpecification(TimeToLiveSpecification.builder()
                            .attributeName("ttl")
                            .enabled(true)
                            .build())
                    .build());
            log.debug("TTL enabled for table {}", tableName);
        } catch (Exception e) {
            log.debug("Could not enable TTL on table {} (may already be active): {}", tableName, e.getMessage());
        }
    }

    @Override
    public boolean tryAcquirePermit() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String minuteKey = getMinuteKey(nowEpochSec);
        String dayKey = getDayKey();
        long minuteTtl = nowEpochSec + 3600L; // 1-hour TTL
        long dayTtl = nowEpochSec + (7L * 86400L); // 7-day TTL
        String nowIso = Instant.now().toString();

        TransactWriteItem updateMinute = TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName(tableName)
                        .key(Map.of("pk", AttributeValue.builder().s(minuteKey).build()))
                        .updateExpression("ADD #cnt :one SET #ttl = :ttl, #upd = :now")
                        .conditionExpression("attribute_not_exists(#cnt) OR #cnt < :maxMinute")
                        .expressionAttributeNames(Map.of(
                                "#cnt", "count",
                                "#ttl", "ttl",
                                "#upd", "updatedAt"
                        ))
                        .expressionAttributeValues(Map.of(
                                ":one", AttributeValue.builder().n("1").build(),
                                ":maxMinute", AttributeValue.builder().n(String.valueOf(maxMinuteLimit)).build(),
                                ":ttl", AttributeValue.builder().n(String.valueOf(minuteTtl)).build(),
                                ":now", AttributeValue.builder().s(nowIso).build()
                        ))
                        .build())
                .build();

        TransactWriteItem updateDay = TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName(tableName)
                        .key(Map.of("pk", AttributeValue.builder().s(dayKey).build()))
                        .updateExpression("ADD #cnt :one SET #ttl = :ttl, #upd = :now")
                        .conditionExpression("attribute_not_exists(#cnt) OR #cnt < :maxDay")
                        .expressionAttributeNames(Map.of(
                                "#cnt", "count",
                                "#ttl", "ttl",
                                "#upd", "updatedAt"
                        ))
                        .expressionAttributeValues(Map.of(
                                ":one", AttributeValue.builder().n("1").build(),
                                ":maxDay", AttributeValue.builder().n(String.valueOf(maxDayLimit)).build(),
                                ":ttl", AttributeValue.builder().n(String.valueOf(dayTtl)).build(),
                                ":now", AttributeValue.builder().s(nowIso).build()
                        ))
                        .build())
                .build();

        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(updateMinute, updateDay)
                .build();

        try {
            dynamoDbClient.transactWriteItems(request);
            log.debug("Consumed 1 Open-Meteo call permit (minuteKey={}, dayKey={})", minuteKey, dayKey);
            return true;
        } catch (TransactionCanceledException e) {
            log.warn("Open-Meteo call budget limit exceeded in DynamoDB. Transaction canceled: minuteLimit={}, dayLimit={}",
                    maxMinuteLimit, maxDayLimit);
            return false;
        } catch (Exception e) {
            log.error("Failed to update Open-Meteo call budget in DynamoDB table {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    @Override
    public long getCallCount(String budgetKey) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("pk", AttributeValue.builder().s(budgetKey).build()))
                    .projectionExpression("#cnt")
                    .expressionAttributeNames(Map.of("#cnt", "count"))
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem() && response.item().containsKey("count")) {
                return Long.parseLong(response.item().get("count").n());
            }
        } catch (Exception e) {
            log.warn("Failed to get call count for key {}: {}", budgetKey, e.getMessage());
        }
        return 0L;
    }

    @Override
    public BudgetStatus getBudgetStatus() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String minuteKey = getMinuteKey(nowEpochSec);
        String dayKey = getDayKey();

        long minCount = getCallCount(minuteKey);
        long dayCount = getCallCount(dayKey);
        boolean allowed = minCount < maxMinuteLimit && dayCount < maxDayLimit;

        return new BudgetStatus(minCount, maxMinuteLimit, dayCount, maxDayLimit, allowed);
    }

    public static String getMinuteKey(long epochSecond) {
        long epochMinute = epochSecond / 60L;
        return "BUDGET#om#min#" + epochMinute;
    }

    public static String getDayKey() {
        String dateStr = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        return "BUDGET#om#day#" + dateStr;
    }

    public String getTableName() {
        return tableName;
    }

    public int getMaxMinuteLimit() {
        return maxMinuteLimit;
    }

    public int getMaxDayLimit() {
        return maxDayLimit;
    }
}
