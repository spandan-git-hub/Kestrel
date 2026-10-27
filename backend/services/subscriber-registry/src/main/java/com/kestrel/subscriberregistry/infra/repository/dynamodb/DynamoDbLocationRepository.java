package com.kestrel.subscriberregistry.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.subscriberregistry.infra.entity.LocationEntity;
import com.kestrel.subscriberregistry.infra.repository.LocationRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbLocationRepository implements LocationRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbLocationRepository.class);

    private final DynamoDbTable<LocationEntity> table;

    public DynamoDbLocationRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_registry_location", "DYNAMODB_REGISTRY_LOCATION");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(LocationEntity.class));
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
    public List<LocationEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public Optional<LocationEntity> findById(String id) {
        LocationEntity entity = table.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(entity);
    }

    @Override
    public LocationEntity save(LocationEntity location) {
        if (location.getCreatedAt() == null) {
            location.setCreatedAt(Instant.now());
        }
        location.setUpdatedAt(Instant.now());
        table.putItem(location);
        return location;
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public void deleteById(String id) {
        table.deleteItem(Key.builder().partitionValue(id).build());
    }

    @Override
    public List<LocationEntity> findByActiveTrue() {
        return findAll().stream()
                .filter(LocationEntity::isActive)
                .toList();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}
