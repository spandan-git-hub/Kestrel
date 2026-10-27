package com.kestrel.rulesengine.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.rulesengine.infra.entity.AlertStateEntity;
import com.kestrel.rulesengine.infra.repository.AlertStateRepository;
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
public class DynamoDbAlertStateRepository implements AlertStateRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbAlertStateRepository.class);

    private final DynamoDbTable<AlertStateEntity> table;

    public DynamoDbAlertStateRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_rules_alert_state", "DYNAMODB_RULES_ALERT_STATE");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(AlertStateEntity.class));
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
    public Optional<AlertStateEntity> findById(String locationId) {
        AlertStateEntity state = table.getItem(Key.builder().partitionValue(locationId).build());
        return Optional.ofNullable(state);
    }

    @Override
    public AlertStateEntity save(AlertStateEntity entity) {
        if (entity.getTtl() == null) {
            entity.setTtl(Instant.now().getEpochSecond() + (90L * 24 * 3600));
        }
        table.putItem(entity);
        return entity;
    }

    @Override
    public List<AlertStateEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}
