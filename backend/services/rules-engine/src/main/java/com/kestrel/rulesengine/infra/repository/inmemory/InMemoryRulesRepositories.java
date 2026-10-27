package com.kestrel.rulesengine.infra.repository.inmemory;

import com.kestrel.rulesengine.infra.entity.AlertHistoryEntity;
import com.kestrel.rulesengine.infra.entity.AlertStateEntity;
import com.kestrel.rulesengine.infra.entity.ForecastSnapshotReplicaEntity;
import com.kestrel.rulesengine.infra.entity.MonitoredLocationReplicaEntity;
import com.kestrel.rulesengine.infra.repository.AlertHistoryRepository;
import com.kestrel.rulesengine.infra.repository.AlertStateRepository;
import com.kestrel.rulesengine.infra.repository.ForecastSnapshotReplicaRepository;
import com.kestrel.rulesengine.infra.repository.MonitoredLocationReplicaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRulesRepositories {

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryAlertStateRepository implements AlertStateRepository {
        private final Map<String, AlertStateEntity> store = new ConcurrentHashMap<>();

        @Override
        public Optional<AlertStateEntity> findById(String locationId) {
            return Optional.ofNullable(store.get(locationId));
        }

        @Override
        public AlertStateEntity save(AlertStateEntity entity) {
            store.put(entity.getLocationId(), entity);
            return entity;
        }

        @Override
        public List<AlertStateEntity> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    @Repository
    public static class InMemoryAlertHistoryRepository implements AlertHistoryRepository {
        private final List<AlertHistoryEntity> store = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public List<AlertHistoryEntity> findByLocationIdOrderByTransitionedAtDesc(String locationId) {
            return store.stream()
                    .filter(h -> locationId.equals(h.getLocationId()))
                    .sorted(Comparator.comparing(AlertHistoryEntity::getTransitionedAt).reversed())
                    .toList();
        }

        @Override
        public AlertHistoryEntity save(AlertHistoryEntity entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public List<AlertHistoryEntity> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    @Repository
    public static class InMemoryForecastSnapshotReplicaRepository implements ForecastSnapshotReplicaRepository {
        private final Map<String, ForecastSnapshotReplicaEntity> store = new ConcurrentHashMap<>();

        @Override
        public Optional<ForecastSnapshotReplicaEntity> findFirstByLocationIdOrderByDataAsOfDesc(String locationId) {
            return Optional.ofNullable(store.get(locationId));
        }

        @Override
        public Optional<ForecastSnapshotReplicaEntity> findById(String locationId) {
            return Optional.ofNullable(store.get(locationId));
        }

        @Override
        public ForecastSnapshotReplicaEntity save(ForecastSnapshotReplicaEntity entity) {
            store.put(entity.getLocationId(), entity);
            return entity;
        }

        @Override
        public List<ForecastSnapshotReplicaEntity> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long count() {
            return store.size();
        }
    }

    @Repository
    public static class InMemoryMonitoredLocationReplicaRepository implements MonitoredLocationReplicaRepository {
        private final Map<String, MonitoredLocationReplicaEntity> store = new ConcurrentHashMap<>();

        @Override
        public List<MonitoredLocationReplicaEntity> findByActiveTrue() {
            return store.values().stream().filter(MonitoredLocationReplicaEntity::isActive).toList();
        }

        @Override
        public Optional<MonitoredLocationReplicaEntity> findById(String locationId) {
            return Optional.ofNullable(store.get(locationId));
        }

        @Override
        public MonitoredLocationReplicaEntity save(MonitoredLocationReplicaEntity entity) {
            store.put(entity.getLocationId(), entity);
            return entity;
        }

        @Override
        public List<MonitoredLocationReplicaEntity> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long count() {
            return store.size();
        }
    }
}
