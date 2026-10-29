package com.kestrel.alertfanout.infra.repository.dynamodb;

import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import com.kestrel.alertfanout.infra.entity.CommunityReport;
import com.kestrel.alertfanout.infra.entity.DeliveryLog;
import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.alertfanout.infra.repository.ActiveAlertRepository;
import com.kestrel.alertfanout.infra.repository.CommunityReportRepository;
import com.kestrel.alertfanout.infra.repository.DeliveryLogRepository;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.alertfanout.infra.repository.SubscriberReplicaRepository;
import com.kestrel.common.core.config.dynamodb.DynamoDbTableNameResolver;
import com.kestrel.common.events.Channel;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DynamoDbFanoutRepositories {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbFanoutRepositories.class);

    @Repository
    @Primary
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
    public static class DynamoDbDeliveryLogRepository implements DeliveryLogRepository {
        private final DynamoDbTable<DeliveryLog> table;

        public DynamoDbDeliveryLogRepository(
                DynamoDbEnhancedClient enhancedClient,
                DynamoDbTableNameResolver tableNameResolver) {
            String tableName = tableNameResolver.resolve("kestrel_fanout_delivery", "DYNAMODB_FANOUT_DELIVERY");
            this.table = enhancedClient.table(tableName, TableSchema.fromBean(DeliveryLog.class));
        }

        @PostConstruct
        public void initTable() {
            try {
                table.createTable();
            } catch (ResourceInUseException ignored) {
            } catch (Exception e) {
                log.warn("Could not create DynamoDB table {}: {}", table.tableName(), e.getMessage());
            }
        }

        @Override
        public List<DeliveryLog> findByAlertId(String alertId) {
            try {
                DynamoDbIndex<DeliveryLog> index = table.index("alertId-index");
                QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(alertId).build());
                return index.query(query).stream()
                        .flatMap(page -> page.items().stream())
                        .toList();
            } catch (Exception e) {
                return table.scan().items().stream()
                        .filter(l -> alertId.equals(l.getAlertId()))
                        .toList();
            }
        }

        @Override
        public DeliveryLog save(DeliveryLog logItem) {
            table.putItem(logItem);
            return logItem;
        }

        @Override
        public List<DeliveryLog> findAll() {
            return table.scan().items().stream().toList();
        }

        @Override
        public long count() {
            return findAll().size();
        }

        @Override
        public void deleteAll() {
            findAll().forEach(l -> {
                if (l.getDeliveryId() != null) {
                    table.deleteItem(Key.builder().partitionValue(l.getDeliveryId()).build());
                }
            });
        }
    }

    @Repository
    @Primary
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
    public static class DynamoDbCommunityReportRepository implements CommunityReportRepository {
        private final DynamoDbTable<CommunityReport> table;

        public DynamoDbCommunityReportRepository(
                DynamoDbEnhancedClient enhancedClient,
                DynamoDbTableNameResolver tableNameResolver) {
            String tableName = tableNameResolver.resolve("kestrel_reports", "DYNAMODB_REPORTS");
            this.table = enhancedClient.table(tableName, TableSchema.fromBean(CommunityReport.class));
        }

        @PostConstruct
        public void initTable() {
            try {
                table.createTable();
            } catch (ResourceInUseException ignored) {
            } catch (Exception e) {
                log.warn("Could not create DynamoDB table {}: {}", table.tableName(), e.getMessage());
            }
        }

        @Override
        public List<CommunityReport> findByLocationIdOrderByCreatedAtDesc(String locationId) {
            try {
                DynamoDbIndex<CommunityReport> index = table.index("locationId-index");
                QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build());
                return index.query(query).stream()
                        .flatMap(page -> page.items().stream())
                        .sorted(Comparator.comparing(CommunityReport::getCreatedAt).reversed())
                        .toList();
            } catch (Exception e) {
                return table.scan().items().stream()
                        .filter(r -> locationId.equals(r.getLocationId()))
                        .sorted(Comparator.comparing(CommunityReport::getCreatedAt).reversed())
                        .toList();
            }
        }

        @Override
        public CommunityReport save(CommunityReport report) {
            table.putItem(report);
            return report;
        }

        @Override
        public List<CommunityReport> findAll() {
            return table.scan().items().stream().toList();
        }

        @Override
        public long count() {
            return findAll().size();
        }

        @Override
        public void deleteAll() {
            findAll().forEach(r -> {
                if (r.getReportId() != null) {
                    table.deleteItem(Key.builder().partitionValue(r.getReportId()).build());
                }
            });
        }
    }

    @Repository
    @Primary
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
    public static class DynamoDbActiveAlertRepository implements ActiveAlertRepository {
        private final DynamoDbTable<ActiveAlertEntity> table;

        public DynamoDbActiveAlertRepository(
                DynamoDbEnhancedClient enhancedClient,
                DynamoDbTableNameResolver tableNameResolver) {
            String tableName = tableNameResolver.resolve("kestrel_fanout_active_alert", null);
            this.table = enhancedClient.table(tableName, TableSchema.fromBean(ActiveAlertEntity.class));
        }

        @PostConstruct
        public void initTable() {
            try {
                table.createTable();
            } catch (ResourceInUseException ignored) {
            } catch (Exception e) {
                log.warn("Could not create DynamoDB table {}: {}", table.tableName(), e.getMessage());
            }
        }

        @Override
        public Optional<ActiveAlertEntity> findById(String locationId) {
            ActiveAlertEntity alert = table.getItem(Key.builder().partitionValue(locationId).build());
            return Optional.ofNullable(alert);
        }

        @Override
        public ActiveAlertEntity save(ActiveAlertEntity entity) {
            table.putItem(entity);
            return entity;
        }

        @Override
        public void deleteById(String locationId) {
            table.deleteItem(Key.builder().partitionValue(locationId).build());
        }

        @Override
        public List<ActiveAlertEntity> findAll() {
            return table.scan().items().stream().toList();
        }

        @Override
        public long count() {
            return findAll().size();
        }
    }

    @Repository
    @Primary
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
    public static class DynamoDbLocationReplicaRepository implements LocationReplicaRepository {
        private final DynamoDbTable<LocationReplica> table;

        public DynamoDbLocationReplicaRepository(
                DynamoDbEnhancedClient enhancedClient,
                DynamoDbTableNameResolver tableNameResolver) {
            String tableName = tableNameResolver.resolve("kestrel_fanout_location_replica", null);
            this.table = enhancedClient.table(tableName, TableSchema.fromBean(LocationReplica.class));
        }

        @PostConstruct
        public void initTable() {
            try {
                table.createTable();
            } catch (ResourceInUseException ignored) {
            } catch (Exception e) {
                log.warn("Could not create DynamoDB table {}: {}", table.tableName(), e.getMessage());
            }
        }

        @Override
        public Optional<LocationReplica> findById(String locationId) {
            LocationReplica loc = table.getItem(Key.builder().partitionValue(locationId).build());
            return Optional.ofNullable(loc);
        }

        @Override
        public LocationReplica save(LocationReplica replica) {
            table.putItem(replica);
            return replica;
        }

        @Override
        public List<LocationReplica> findAll() {
            return table.scan().items().stream().toList();
        }

        @Override
        public long count() {
            return findAll().size();
        }
    }

    @Repository
    @Primary
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "dynamodb", matchIfMissing = true)
    public static class DynamoDbSubscriberReplicaRepository implements SubscriberReplicaRepository {
        private final DynamoDbTable<SubscriberReplica> table;

        public DynamoDbSubscriberReplicaRepository(
                DynamoDbEnhancedClient enhancedClient,
                DynamoDbTableNameResolver tableNameResolver) {
            String tableName = tableNameResolver.resolve("kestrel_fanout_subscriber_replica", "DYNAMODB_FANOUT_REPLICA");
            this.table = enhancedClient.table(tableName, TableSchema.fromBean(SubscriberReplica.class));
        }

        @PostConstruct
        public void initTable() {
            try {
                table.createTable();
            } catch (ResourceInUseException ignored) {
            } catch (Exception e) {
                log.warn("Could not create DynamoDB table {}: {}", table.tableName(), e.getMessage());
            }
        }

        @Override
        public SubscriberReplica save(SubscriberReplica replica) {
            table.putItem(replica);
            return replica;
        }

        @Override
        public Optional<SubscriberReplica> findByLocationIdAndToken(String locationId, String token) {
            SubscriberReplica item = table.getItem(Key.builder().partitionValue(locationId).sortValue(token).build());
            return Optional.ofNullable(item);
        }

        @Override
        public List<SubscriberReplica> findByLocationId(String locationId) {
            try {
                QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(locationId).build());
                return table.query(query).items().stream().toList();
            } catch (Exception e) {
                return table.scan().items().stream()
                        .filter(s -> locationId.equals(s.getLocationId()))
                        .toList();
            }
        }

        @Override
        public List<SubscriberReplica> findByChannel(Channel channel) {
            try {
                DynamoDbIndex<SubscriberReplica> index = table.index("channel-index");
                QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(channel.name()).build());
                return index.query(query).stream()
                        .flatMap(page -> page.items().stream())
                        .toList();
            } catch (Exception e) {
                return table.scan().items().stream()
                        .filter(s -> channel == s.getChannel())
                        .toList();
            }
        }

        @Override
        public void deleteByLocationIdAndToken(String locationId, String token) {
            table.deleteItem(Key.builder().partitionValue(locationId).sortValue(token).build());
        }

        @Override
        public void deleteByToken(String token) {
            table.scan().items().stream()
                    .filter(s -> token.equals(s.getToken()))
                    .forEach(s -> table.deleteItem(Key.builder().partitionValue(s.getLocationId()).sortValue(s.getToken()).build()));
        }

        @Override
        public List<SubscriberReplica> findAll() {
            return table.scan().items().stream().toList();
        }

        @Override
        public long count() {
            return findAll().size();
        }

        @Override
        public void deleteAll() {
            findAll().forEach(s -> table.deleteItem(Key.builder().partitionValue(s.getLocationId()).sortValue(s.getToken()).build()));
        }
    }
}
