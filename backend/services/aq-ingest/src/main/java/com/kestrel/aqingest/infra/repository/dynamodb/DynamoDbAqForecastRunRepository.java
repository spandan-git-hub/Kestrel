package com.kestrel.aqingest.infra.repository.dynamodb;

import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.repository.AqForecastRunRepository;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
public class DynamoDbAqForecastRunRepository implements AqForecastRunRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbAqForecastRunRepository.class);

    private final DynamoDbTable<AqForecastRunEntity> table;

    public DynamoDbAqForecastRunRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_aq_forecast_run", "DYNAMODB_AQ_FORECAST_RUN");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(AqForecastRunEntity.class));
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
    public void save(AqForecastRunEntity entity) {
        table.putItem(entity);
    }

    @Override
    public Optional<AqForecastRunEntity> findLatestByLocationId(String locationId) {
        return findByLocationId(locationId).stream()
                .max(Comparator.comparing(AqForecastRunEntity::getFetchedAt));
    }

    @Override
    public List<AqForecastRunEntity> findByLocationId(String locationId) {
        return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build()))
                .items()
                .stream()
                .toList();
    }
}

