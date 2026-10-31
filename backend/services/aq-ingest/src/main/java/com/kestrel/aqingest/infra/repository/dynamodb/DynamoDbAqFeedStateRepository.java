package com.kestrel.aqingest.infra.repository.dynamodb;

import com.kestrel.aqingest.infra.entity.AqFeedStateEntity;
import com.kestrel.aqingest.infra.repository.AqFeedStateRepository;
import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbAqFeedStateRepository implements AqFeedStateRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbAqFeedStateRepository.class);

    private final DynamoDbTable<AqFeedStateEntity> table;

    public DynamoDbAqFeedStateRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_aq_feed_state", "DYNAMODB_AQ_FEED_STATE");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(AqFeedStateEntity.class));
    }

    @PostConstruct
    public void initTable() {
        try {
            table.createTable();
            log.info("Initialized DynamoDB table {}", table.tableName());
        } catch (ResourceInUseException e) {
            log.debug("DynamoDB table {} already exists", table.tableName());
        } catch (Exception e) {
            log.warn("Could not auto-create DynamoDB table {}: {}", table.tableName(), e.getMessage());
        }
    }

    @Override
    public void save(AqFeedStateEntity entity) {
        table.putItem(entity);
    }

    @Override
    public Optional<AqFeedStateEntity> findByName(String feedName) {
        AqFeedStateEntity item = table.getItem(Key.builder().partitionValue(feedName).build());
        return Optional.ofNullable(item);
    }
}

