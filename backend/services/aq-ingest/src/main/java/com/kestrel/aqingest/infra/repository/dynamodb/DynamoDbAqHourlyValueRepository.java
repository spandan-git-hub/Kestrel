package com.kestrel.aqingest.infra.repository.dynamodb;

import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.repository.AqHourlyValueRepository;
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
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.util.List;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbAqHourlyValueRepository implements AqHourlyValueRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbAqHourlyValueRepository.class);

    private final DynamoDbTable<AqHourlyValueEntity> table;

    public DynamoDbAqHourlyValueRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_aq_hourly_value", "DYNAMODB_AQ_HOURLY_VALUE");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(AqHourlyValueEntity.class));
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
    public void save(AqHourlyValueEntity entity) {
        table.putItem(entity);
    }

    @Override
    public void saveAll(List<AqHourlyValueEntity> entities) {
        for (AqHourlyValueEntity entity : entities) {
            table.putItem(entity);
        }
    }

    @Override
    public List<AqHourlyValueEntity> findByLocationId(String locationId) {
        return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build()))
                .items()
                .stream()
                .toList();
    }

    @Override
    public List<AqHourlyValueEntity> findByLocationIdAndTimeRange(String locationId, String startTime, String endTime) {
        return table.query(QueryConditional.sortBetween(
                        Key.builder().partitionValue(locationId).sortValue(startTime).build(),
                        Key.builder().partitionValue(locationId).sortValue(endTime).build()))
                .items()
                .stream()
                .toList();
    }
}

