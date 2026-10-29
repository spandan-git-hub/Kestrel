package com.kestrel.floodingest.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.floodingest.infra.entity.FloodThresholdEntity;
import com.kestrel.floodingest.infra.repository.FloodThresholdRepository;
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

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbFloodThresholdRepository implements FloodThresholdRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbFloodThresholdRepository.class);

    private final DynamoDbTable<FloodThresholdEntity> table;

    public DynamoDbFloodThresholdRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_flood_threshold", "DYNAMODB_FLOOD_THRESHOLD");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(FloodThresholdEntity.class));
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
    public Optional<FloodThresholdEntity> findById(String locationId) {
        FloodThresholdEntity entity = table.getItem(Key.builder().partitionValue(locationId).build());
        return Optional.ofNullable(entity);
    }

    @Override
    public FloodThresholdEntity save(FloodThresholdEntity entity) {
        table.putItem(entity);
        return entity;
    }

    @Override
    public List<FloodThresholdEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public void saveAll(Iterable<FloodThresholdEntity> entities) {
        entities.forEach(this::save);
    }

    @Override
    public boolean existsById(String locationId) {
        return findById(locationId).isPresent();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}

