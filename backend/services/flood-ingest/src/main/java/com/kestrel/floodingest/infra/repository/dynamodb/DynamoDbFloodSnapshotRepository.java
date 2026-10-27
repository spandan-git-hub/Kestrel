package com.kestrel.floodingest.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.floodingest.infra.entity.FloodSnapshotEntity;
import com.kestrel.floodingest.infra.repository.FloodSnapshotRepository;
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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbFloodSnapshotRepository implements FloodSnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbFloodSnapshotRepository.class);

    private final DynamoDbTable<FloodSnapshotEntity> table;

    public DynamoDbFloodSnapshotRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_flood_snapshot", "DYNAMODB_FLOOD_SNAPSHOT");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(FloodSnapshotEntity.class));
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
    public Optional<FloodSnapshotEntity> findFirstByLocationIdOrderByDataAsOfDesc(String locationId) {
        try {
            QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build());
            return table.query(r -> r.queryConditional(query).scanIndexForward(false).limit(1))
                    .items().stream().findFirst();
        } catch (Exception e) {
            log.warn("Query failed on table {}, falling back to scan: {}", table.tableName(), e.getMessage());
            return table.scan().items().stream()
                    .filter(s -> locationId.equals(s.getLocationId()))
                    .max(Comparator.comparing(FloodSnapshotEntity::getDataAsOf));
        }
    }

    @Override
    public FloodSnapshotEntity save(FloodSnapshotEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        if (entity.getSnapshotTimestamp() == null && entity.getDataAsOf() != null) {
            entity.setSnapshotTimestamp(entity.getDataAsOf().toString());
        }
        if (entity.getTtl() == null) {
            long epoch = entity.getDataAsOf() != null ? entity.getDataAsOf().getEpochSecond() : Instant.now().getEpochSecond();
            entity.setTtl(epoch + (30L * 24 * 3600));
        }
        table.putItem(entity);
        return entity;
    }

    @Override
    public List<FloodSnapshotEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public Optional<FloodSnapshotEntity> findById(Long id) {
        return findAll().stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}
