package com.kestrel.aqingest.infra.repository.inmemory;

import com.kestrel.aqingest.infra.entity.AqFeedStateEntity;
import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.repository.AqFeedStateRepository;
import com.kestrel.aqingest.infra.repository.AqForecastRunRepository;
import com.kestrel.aqingest.infra.repository.AqHourlyValueRepository;
import com.kestrel.aqingest.infra.repository.AqMonitoredPointRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAqRepositories {

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryAqMonitoredPointRepository implements AqMonitoredPointRepository {
        private final Map<String, AqMonitoredPointEntity> storage = new ConcurrentHashMap<>();

        @Override
        public List<AqMonitoredPointEntity> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<AqMonitoredPointEntity> findByActiveTrue() {
            return storage.values().stream()
                    .filter(AqMonitoredPointEntity::isActive)
                    .toList();
        }

        @Override
        public Optional<AqMonitoredPointEntity> findById(String locationId) {
            return Optional.ofNullable(storage.get(locationId));
        }

        @Override
        public void save(AqMonitoredPointEntity entity) {
            storage.put(entity.getLocationId(), entity);
        }

        @Override
        public void saveAll(List<AqMonitoredPointEntity> entities) {
            entities.forEach(this::save);
        }

        @Override
        public boolean existsById(String locationId) {
            return storage.containsKey(locationId);
        }

        @Override
        public long count() {
            return storage.size();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryAqForecastRunRepository implements AqForecastRunRepository {
        private final Map<String, AqForecastRunEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void save(AqForecastRunEntity entity) {
            storage.put(entity.getLocationId() + "#" + entity.getRunId(), entity);
        }

        @Override
        public Optional<AqForecastRunEntity> findLatestByLocationId(String locationId) {
            return findByLocationId(locationId).stream()
                    .max(Comparator.comparing(AqForecastRunEntity::getFetchedAt));
        }

        @Override
        public List<AqForecastRunEntity> findByLocationId(String locationId) {
            return storage.values().stream()
                    .filter(e -> e.getLocationId().equalsIgnoreCase(locationId))
                    .sorted(Comparator.comparing(AqForecastRunEntity::getFetchedAt).reversed())
                    .toList();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryAqHourlyValueRepository implements AqHourlyValueRepository {
        private final Map<String, AqHourlyValueEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void save(AqHourlyValueEntity entity) {
            storage.put(entity.getLocationId() + "#" + entity.getTime(), entity);
        }

        @Override
        public void saveAll(List<AqHourlyValueEntity> entities) {
            entities.forEach(this::save);
        }

        @Override
        public List<AqHourlyValueEntity> findByLocationId(String locationId) {
            return storage.values().stream()
                    .filter(e -> e.getLocationId().equalsIgnoreCase(locationId))
                    .sorted(Comparator.comparing(AqHourlyValueEntity::getTime))
                    .toList();
        }

        @Override
        public List<AqHourlyValueEntity> findByLocationIdAndTimeRange(String locationId, String startTime, String endTime) {
            return storage.values().stream()
                    .filter(e -> e.getLocationId().equalsIgnoreCase(locationId))
                    .filter(e -> e.getTime().compareTo(startTime) >= 0 && e.getTime().compareTo(endTime) <= 0)
                    .sorted(Comparator.comparing(AqHourlyValueEntity::getTime))
                    .toList();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryAqFeedStateRepository implements AqFeedStateRepository {
        private final Map<String, AqFeedStateEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void save(AqFeedStateEntity entity) {
            storage.put(entity.getFeedName(), entity);
        }

        @Override
        public Optional<AqFeedStateEntity> findByName(String feedName) {
            return Optional.ofNullable(storage.get(feedName));
        }
    }
}

