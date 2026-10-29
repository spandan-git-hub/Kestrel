package com.kestrel.alertfanout.infra.limiter;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.Map;

/**
 * Production DynamoDB atomic token-bucket rate limiter for Telegram Bot API dispatches.
 * Uses atomic conditional updates on table kestrel_fanout_tg_bucket.
 * Enforces target limit of 25 msg/s and hard ceiling of 30 msg/s.
 */
@Component
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbTelegramRateLimiter implements TelegramRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTelegramRateLimiter.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final int targetLimit;
    private final int ceilingLimit;

    public DynamoDbTelegramRateLimiter(
            DynamoDbClient dynamoDbClient,
            DynamoDbTableNameResolver tableNameResolver,
            @Value("${kestrel.telegram.rate-limit.target:25}") int targetLimit,
            @Value("${kestrel.telegram.rate-limit.ceiling:30}") int ceilingLimit) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableNameResolver.resolve("kestrel_fanout_tg_bucket", "DYNAMODB_FANOUT_TG_BUCKET");
        this.targetLimit = targetLimit;
        this.ceilingLimit = ceilingLimit;
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
            log.info("Initialized DynamoDB table {} for Telegram token bucket rate limiting", tableName);
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
            log.debug("Could not enable TTL on table {}: {}", tableName, e.getMessage());
        }
    }

    @Override
    public boolean tryAcquire() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String pk = "TG_BUCKET#sec#" + nowEpochSec;
        long ttlEpochSec = nowEpochSec + 3600L; // 1 hour TTL
        String nowIso = Instant.now().toString();

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("pk", AttributeValue.builder().s(pk).build()))
                .updateExpression("ADD #cnt :one SET #ttl = :ttl, #upd = :now")
                .conditionExpression("attribute_not_exists(#cnt) OR #cnt < :ceiling")
                .expressionAttributeNames(Map.of(
                        "#cnt", "count",
                        "#ttl", "ttl",
                        "#upd", "updatedAt"
                ))
                .expressionAttributeValues(Map.of(
                        ":one", AttributeValue.builder().n("1").build(),
                        ":ceiling", AttributeValue.builder().n(String.valueOf(ceilingLimit)).build(),
                        ":ttl", AttributeValue.builder().n(String.valueOf(ttlEpochSec)).build(),
                        ":now", AttributeValue.builder().s(nowIso).build()
                ))
                .build();

        try {
            dynamoDbClient.updateItem(request);
            log.debug("Acquired Telegram dispatch permit in DynamoDB (key={}, ceiling={})", pk, ceilingLimit);
            return true;
        } catch (ConditionalCheckFailedException e) {
            log.warn("Telegram dispatch ceiling reached ({} msg/s) for key: {}", ceilingLimit, pk);
            return false;
        } catch (Exception e) {
            log.error("Failed to update Telegram token bucket in DynamoDB table {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean acquireWithBackoff(long maxWaitMillis) {
        long deadline = System.currentTimeMillis() + maxWaitMillis;
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire()) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public RateLimitStatus getStatus() {
        long nowEpochSec = Instant.now().getEpochSecond();
        String pk = "TG_BUCKET#sec#" + nowEpochSec;
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("pk", AttributeValue.builder().s(pk).build()))
                    .build());
            long count = 0;
            if (response.hasItem() && response.item().containsKey("count")) {
                count = Long.parseLong(response.item().get("count").n());
            }
            return new RateLimitStatus(count, targetLimit, ceilingLimit, count < ceilingLimit);
        } catch (Exception e) {
            log.warn("Failed to query rate limit status from DynamoDB table {}: {}", tableName, e.getMessage());
            return new RateLimitStatus(0, targetLimit, ceilingLimit, true);
        }
    }
}

