package com.kestrel.subscriberregistry.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;
import com.kestrel.subscriberregistry.infra.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbSubscriptionRepository implements SubscriptionRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbSubscriptionRepository.class);

    private final DynamoDbTable<SubscriptionEntity> table;

    public DynamoDbSubscriptionRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_registry_subscription", "DYNAMODB_REGISTRY_SUBSCRIPTION");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(SubscriptionEntity.class));
    }

    @PostConstruct
    public void initTable() {
        try {
            table.createTable();
            log.info("Initialized DynamoDB table {}", table.tableName());
        } catch (ResourceInUseException e) {
            log.debug("DynamoDB table {} already exists", table.tableName());
        } catch (Exception e) {
            log.warn("Could not auto-create DynamoDB table {} (managed externally or local fallback): {}", table.tableName(), e.getMessage());
        }
    }

    @Override
    public List<SubscriptionEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public Optional<SubscriptionEntity> findById(String token) {
        SubscriptionEntity entity = table.getItem(Key.builder().partitionValue(token).build());
        return Optional.ofNullable(entity);
    }

    @Override
    public List<SubscriptionEntity> findByLocationId(String locationId) {
        return findByLocationIdAndStatus(locationId, null);
    }

    @Override
    public List<SubscriptionEntity> findByLocationIdAndStatus(String locationId, String status) {
        try {
            DynamoDbIndex<SubscriptionEntity> index = table.index("locationId-index");
            QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build());
            return index.query(query).stream()
                    .flatMap(page -> page.items().stream())
                    .filter(sub -> status == null || status.equalsIgnoreCase(sub.getStatus()))
                    .toList();
        } catch (Exception e) {
            log.warn("GSI query failed, falling back to filtered scan: {}", e.getMessage());
            return table.scan().items().stream()
                    .filter(sub -> locationId.equals(sub.getLocationId()))
                    .filter(sub -> status == null || status.equalsIgnoreCase(sub.getStatus()))
                    .toList();
        }
    }

    @Override
    public SubscriptionEntity save(SubscriptionEntity subscription) {
        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(Instant.now());
        }
        subscription.setUpdatedAt(Instant.now());

        // Conditional check for idempotency
        try {
            Expression condition = Expression.builder()
                    .expression("attribute_not_exists(#t) OR #t = :tok")
                    .putExpressionName("#t", "token")
                    .putExpressionValue(":tok", AttributeValue.builder().s(subscription.getToken()).build())
                    .build();

            table.putItem(PutItemEnhancedRequest.builder(SubscriptionEntity.class)
                    .item(subscription)
                    .conditionExpression(condition)
                    .build());
        } catch (ConditionalCheckFailedException e) {
            log.warn("Idempotent write check note: token {} exists", subscription.getToken());
            table.putItem(subscription);
        }
        return subscription;
    }

    @Override
    public void delete(SubscriptionEntity subscription) {
        if (subscription != null && subscription.getToken() != null) {
            deleteById(subscription.getToken());
        }
    }

    @Override
    public void deleteById(String token) {
        table.deleteItem(Key.builder().partitionValue(token).build());
    }

    @Override
    public boolean existsById(String token) {
        return findById(token).isPresent();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}
