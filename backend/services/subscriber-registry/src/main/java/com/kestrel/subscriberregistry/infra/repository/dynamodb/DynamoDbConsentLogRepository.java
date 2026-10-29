package com.kestrel.subscriberregistry.infra.repository.dynamodb;

import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.subscriberregistry.infra.entity.ConsentLogEntity;
import com.kestrel.subscriberregistry.infra.repository.ConsentLogRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
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
public class DynamoDbConsentLogRepository implements ConsentLogRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConsentLogRepository.class);

    private final DynamoDbTable<ConsentLogEntity> table;

    public DynamoDbConsentLogRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTableNameResolver tableNameResolver) {
        String tableName = tableNameResolver.resolve("kestrel_registry_consent_log", "DYNAMODB_REGISTRY_CONSENT_LOG");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(ConsentLogEntity.class));
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
    public ConsentLogEntity save(ConsentLogEntity consentLog) {
        table.putItem(consentLog);
        return consentLog;
    }

    @Override
    public Optional<ConsentLogEntity> findById(String consentId) {
        ConsentLogEntity entity = table.getItem(Key.builder().partitionValue(consentId).build());
        return Optional.ofNullable(entity);
    }

    @Override
    public List<ConsentLogEntity> findByToken(String token) {
        try {
            DynamoDbIndex<ConsentLogEntity> index = table.index("token-index");
            return index.query(r -> r.queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(token).build())
            )).stream().flatMap(p -> p.items().stream()).toList();
        } catch (Exception e) {
            log.debug("Index query failed for token {}, falling back to scan: {}", token, e.getMessage());
            return findAll().stream()
                    .filter(c -> token.equals(c.getToken()))
                    .toList();
        }
    }

    @Override
    public List<ConsentLogEntity> findAll() {
        return table.scan().items().stream().toList();
    }

    @Override
    public long count() {
        return findAll().size();
    }
}

