package com.kestrel.heatingest.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.heatingest.infra.entity.HeatForecastDayEntity;
import com.kestrel.heatingest.infra.repository.HeatForecastDayRepository;
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
public class DynamoDbHeatForecastDayRepository implements HeatForecastDayRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbHeatForecastDayRepository.class);

    private final DynamoDbTable<HeatForecastDayEntity> table;

    public DynamoDbHeatForecastDayRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_heat_forecast_day", "DYNAMODB_HEAT_FORECAST_DAY");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(HeatForecastDayEntity.class));
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
    public void save(HeatForecastDayEntity entity) {
        table.putItem(entity);
    }

    @Override
    public void saveAll(List<HeatForecastDayEntity> entities) {
        for (HeatForecastDayEntity entity : entities) {
            table.putItem(entity);
        }
    }

    @Override
    public List<HeatForecastDayEntity> findByLocationId(String locationId) {
        return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build()))
                .items()
                .stream()
                .sorted(Comparator.comparing(HeatForecastDayEntity::getDate))
                .toList();
    }

    @Override
    public Optional<HeatForecastDayEntity> findByLocationIdAndDate(String locationId, String date) {
        HeatForecastDayEntity item = table.getItem(Key.builder()
                .partitionValue(locationId)
                .sortValue(date)
                .build());
        return Optional.ofNullable(item);
    }
}

