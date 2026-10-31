package com.kestrel.heatingest.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.heatingest.infra.entity.HeatNormalEntity;
import com.kestrel.heatingest.infra.repository.HeatNormalRepository;
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
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbHeatNormalRepository implements HeatNormalRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbHeatNormalRepository.class);

    private final DynamoDbTable<HeatNormalEntity> table;

    public DynamoDbHeatNormalRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_heat_normal", "DYNAMODB_HEAT_NORMAL");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(HeatNormalEntity.class));
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
    public Optional<HeatNormalEntity> findByLocationIdAndDayOfYear(String locationId, int dayOfYear) {
        HeatNormalEntity item = table.getItem(Key.builder()
                .partitionValue(locationId)
                .sortValue(dayOfYear)
                .build());
        return Optional.ofNullable(item);
    }

    @Override
    public List<HeatNormalEntity> findByLocationId(String locationId) {
        return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build()))
                .items()
                .stream()
                .toList();
    }

    @Override
    public void save(HeatNormalEntity entity) {
        table.putItem(entity);
    }

    @Override
    public void saveAll(List<HeatNormalEntity> entities) {
        for (HeatNormalEntity entity : entities) {
            table.putItem(entity);
        }
    }

    @Override
    public long countByLocationId(String locationId) {
        return findByLocationId(locationId).size();
    }

    @Override
    public long count() {
        return table.scan().items().stream().count();
    }
}

