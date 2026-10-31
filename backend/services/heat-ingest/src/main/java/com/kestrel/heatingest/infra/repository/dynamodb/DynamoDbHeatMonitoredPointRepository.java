package com.kestrel.heatingest.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;
import com.kestrel.heatingest.infra.repository.HeatMonitoredPointRepository;
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
public class DynamoDbHeatMonitoredPointRepository implements HeatMonitoredPointRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbHeatMonitoredPointRepository.class);

    private final DynamoDbTable<HeatMonitoredPointEntity> table;

    public DynamoDbHeatMonitoredPointRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_heat_monitored_point", "DYNAMODB_HEAT_MONITORED_POINT");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(HeatMonitoredPointEntity.class));
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
    public List<HeatMonitoredPointEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public List<HeatMonitoredPointEntity> findByActiveTrue() {
        return findAll().stream()
                .filter(HeatMonitoredPointEntity::isActive)
                .toList();
    }

    @Override
    public Optional<HeatMonitoredPointEntity> findById(String locationId) {
        HeatMonitoredPointEntity item = table.getItem(Key.builder().partitionValue(locationId).build());
        return Optional.ofNullable(item);
    }

    @Override
    public void save(HeatMonitoredPointEntity entity) {
        table.putItem(entity);
    }

    @Override
    public void saveAll(List<HeatMonitoredPointEntity> entities) {
        for (HeatMonitoredPointEntity entity : entities) {
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

