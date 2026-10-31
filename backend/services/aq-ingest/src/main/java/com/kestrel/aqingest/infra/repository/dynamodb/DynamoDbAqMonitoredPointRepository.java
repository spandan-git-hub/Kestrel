package com.kestrel.aqingest.infra.repository.dynamodb;

import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.repository.AqMonitoredPointRepository;
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

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbAqMonitoredPointRepository implements AqMonitoredPointRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbAqMonitoredPointRepository.class);

    private final DynamoDbTable<AqMonitoredPointEntity> table;

    public DynamoDbAqMonitoredPointRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_aq_monitored_point", "DYNAMODB_AQ_MONITORED_POINT");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(AqMonitoredPointEntity.class));
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
    public List<AqMonitoredPointEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public List<AqMonitoredPointEntity> findByActiveTrue() {
        return findAll().stream()
                .filter(AqMonitoredPointEntity::isActive)
                .toList();
    }

    @Override
    public Optional<AqMonitoredPointEntity> findById(String locationId) {
        AqMonitoredPointEntity item = table.getItem(Key.builder().partitionValue(locationId).build());
        return Optional.ofNullable(item);
    }

    @Override
    public void save(AqMonitoredPointEntity entity) {
        table.putItem(entity);
    }

    @Override
    public void saveAll(List<AqMonitoredPointEntity> entities) {
        for (AqMonitoredPointEntity entity : entities) {
            table.putItem(entity);
        }
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

