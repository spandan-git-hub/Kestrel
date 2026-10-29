package com.kestrel.alertfanout.infra.repository.inmemory;

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
import com.kestrel.common.events.Channel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryFanoutRepositories {

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryDeliveryLogRepository implements DeliveryLogRepository {
        private final List<DeliveryLog> store = new CopyOnWriteArrayList<>();

        @Override
        public List<DeliveryLog> findByAlertId(String alertId) {
            return store.stream()
                    .filter(l -> alertId.equals(l.getAlertId()))
                    .toList();
        }

        @Override
        public DeliveryLog save(DeliveryLog logItem) {
            store.add(logItem);
            return logItem;
        }

        @Override
        public List<DeliveryLog> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteAll() {
            store.clear();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryCommunityReportRepository implements CommunityReportRepository {
        private final List<CommunityReport> store = new CopyOnWriteArrayList<>();

        @Override
        public List<CommunityReport> findByLocationIdOrderByCreatedAtDesc(String locationId) {
            return store.stream()
                    .filter(r -> locationId.equals(r.getLocationId()))
                    .sorted(Comparator.comparing(CommunityReport::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public CommunityReport save(CommunityReport report) {
            store.add(report);
            return report;
        }

        @Override
        public List<CommunityReport> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteAll() {
            store.clear();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryActiveAlertRepository implements ActiveAlertRepository {
        private final Map<String, ActiveAlertEntity> store = new ConcurrentHashMap<>();

        @Override
        public Optional<ActiveAlertEntity> findById(String locationId) {
            return Optional.ofNullable(store.get(locationId));
        }

        @Override
        public ActiveAlertEntity save(ActiveAlertEntity entity) {
            store.put(entity.getLocationId(), entity);
            return entity;
        }

        @Override
        public void deleteById(String locationId) {
            store.remove(locationId);
        }

        @Override
        public List<ActiveAlertEntity> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteAll() {
            store.clear();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryLocationReplicaRepository implements LocationReplicaRepository {
        private final Map<String, LocationReplica> store = new ConcurrentHashMap<>();

        @Override
        public Optional<LocationReplica> findById(String locationId) {
            return Optional.ofNullable(store.get(locationId));
        }

        @Override
        public LocationReplica save(LocationReplica replica) {
            store.put(replica.getLocationId(), replica);
            return replica;
        }

        @Override
        public List<LocationReplica> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemorySubscriberReplicaRepository implements SubscriberReplicaRepository {
        private final Map<String, SubscriberReplica> store = new ConcurrentHashMap<>();

        private String key(String locationId, String token) {
            return locationId + "#" + token;
        }

        @Override
        public SubscriberReplica save(SubscriberReplica replica) {
            store.put(key(replica.getLocationId(), replica.getToken()), replica);
            return replica;
        }

        @Override
        public Optional<SubscriberReplica> findByLocationIdAndToken(String locationId, String token) {
            return Optional.ofNullable(store.get(key(locationId, token)));
        }

        @Override
        public List<SubscriberReplica> findByLocationId(String locationId) {
            return store.values().stream()
                    .filter(s -> locationId.equals(s.getLocationId()))
                    .toList();
        }

        @Override
        public List<SubscriberReplica> findByChannel(Channel channel) {
            return store.values().stream()
                    .filter(s -> channel == s.getChannel())
                    .toList();
        }

        @Override
        public void deleteByLocationIdAndToken(String locationId, String token) {
            store.remove(key(locationId, token));
        }

        @Override
        public void deleteByToken(String token) {
            store.entrySet().removeIf(entry -> token.equals(entry.getValue().getToken()));
        }

        @Override
        public List<SubscriberReplica> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteAll() {
            store.clear();
        }
    }
}
