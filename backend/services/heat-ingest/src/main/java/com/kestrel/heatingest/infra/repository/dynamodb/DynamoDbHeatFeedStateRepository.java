package com.kestrel.heatingest.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.heatingest.infra.entity.HeatFeedStateEntity;
import com.kestrel.heatingest.infra.repository.HeatFeedStateRepository;
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
public class DynamoDbHeatFeedStateRepository implements HeatFeedStateRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbHeatFeedStateRepository.class);

    private final DynamoDbTable<HeatFeedStateEntity> table;

    public DynamoDbHeatFeedStateRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_heat_feed_state", "DYNAMODB_HEAT_FEED_STATE");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(HeatFeedStateEntity.class));
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
    public void save(HeatFeedStateEntity entity) {
        table.putItem(entity);
    }

    @Override
    public Optional<HeatFeedStateEntity> findByName(String feedName) {
        HeatFeedStateEntity item = table.getItem(Key.builder().partitionValue(feedName).build());
        return Optional.ofNullable(item);
    }
}

