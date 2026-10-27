package com.kestrel.floodingest.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import com.kestrel.floodingest.infra.repository.MonitoredPointReplicaRepository;
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
public class DynamoDbMonitoredPointReplicaRepository implements MonitoredPointReplicaRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbMonitoredPointReplicaRepository.class);

    private final DynamoDbTable<MonitoredPointReplica> table;

    public DynamoDbMonitoredPointReplicaRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_flood_monitored_point", "DYNAMODB_FLOOD_POINT");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(MonitoredPointReplica.class));
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
    public List<MonitoredPointReplica> findByActiveTrue() {
        return findAll().stream()
                .filter(MonitoredPointReplica::isActive)
                .toList();
    }

    @Override
    public Optional<MonitoredPointReplica> findById(String locationId) {
        MonitoredPointReplica replica = table.getItem(Key.builder().partitionValue(locationId).build());
        return Optional.ofNullable(replica);
    }

    @Override
    public MonitoredPointReplica save(MonitoredPointReplica point) {
        table.putItem(point);
        return point;
    }

    @Override
    public List<MonitoredPointReplica> findAll() {
        return table.scan().items().stream().toList();
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
