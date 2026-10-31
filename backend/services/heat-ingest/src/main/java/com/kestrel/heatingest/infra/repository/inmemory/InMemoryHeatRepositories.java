package com.kestrel.heatingest.infra.repository.inmemory;

import com.kestrel.heatingest.infra.entity.HeatFeedStateEntity;
import com.kestrel.heatingest.infra.entity.HeatForecastDayEntity;
import com.kestrel.heatingest.infra.entity.HeatForecastRunEntity;
import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;
import com.kestrel.heatingest.infra.entity.HeatNormalEntity;
import com.kestrel.heatingest.infra.repository.HeatFeedStateRepository;
import com.kestrel.heatingest.infra.repository.HeatForecastDayRepository;
import com.kestrel.heatingest.infra.repository.HeatForecastRunRepository;
import com.kestrel.heatingest.infra.repository.HeatMonitoredPointRepository;
import com.kestrel.heatingest.infra.repository.HeatNormalRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryHeatRepositories {

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryHeatNormalRepository implements HeatNormalRepository {
        private final Map<String, HeatNormalEntity> storage = new ConcurrentHashMap<>();

        @Override
        public Optional<HeatNormalEntity> findByLocationIdAndDayOfYear(String locationId, int dayOfYear) {
            return Optional.ofNullable(storage.get(locationId + "#" + dayOfYear));
        }

        @Override
        public List<HeatNormalEntity> findByLocationId(String locationId) {
            return storage.values().stream()
                    .filter(e -> e.getLocationId().equalsIgnoreCase(locationId))
                    .sorted(Comparator.comparing(HeatNormalEntity::getDayOfYear))
                    .toList();
        }

        @Override
        public void save(HeatNormalEntity entity) {
            storage.put(entity.getLocationId() + "#" + entity.getDayOfYear(), entity);
        }

        @Override
        public void saveAll(List<HeatNormalEntity> entities) {
            entities.forEach(this::save);
        }

        @Override
        public long countByLocationId(String locationId) {
            return findByLocationId(locationId).size();
        }

        @Override
        public long count() {
            return storage.size();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryHeatMonitoredPointRepository implements HeatMonitoredPointRepository {
        private final Map<String, HeatMonitoredPointEntity> storage = new ConcurrentHashMap<>();

        @Override
        public List<HeatMonitoredPointEntity> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<HeatMonitoredPointEntity> findByActiveTrue() {
            return storage.values().stream()
                    .filter(HeatMonitoredPointEntity::isActive)
                    .toList();
        }

        @Override
        public Optional<HeatMonitoredPointEntity> findById(String locationId) {
            return Optional.ofNullable(storage.get(locationId));
        }

        @Override
        public void save(HeatMonitoredPointEntity entity) {
            storage.put(entity.getLocationId(), entity);
        }

        @Override
        public void saveAll(List<HeatMonitoredPointEntity> entities) {
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
    public static class InMemoryHeatForecastRunRepository implements HeatForecastRunRepository {
        private final Map<String, HeatForecastRunEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void save(HeatForecastRunEntity entity) {
            storage.put(entity.getLocationId() + "#" + entity.getRunId(), entity);
        }

        @Override
        public Optional<HeatForecastRunEntity> findLatestByLocationId(String locationId) {
            return findByLocationId(locationId).stream()
                    .max(Comparator.comparing(HeatForecastRunEntity::getFetchedAt));
        }

        @Override
        public List<HeatForecastRunEntity> findByLocationId(String locationId) {
            return storage.values().stream()
                    .filter(e -> e.getLocationId().equalsIgnoreCase(locationId))
                    .toList();
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryHeatForecastDayRepository implements HeatForecastDayRepository {
        private final Map<String, HeatForecastDayEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void save(HeatForecastDayEntity entity) {
            storage.put(entity.getLocationId() + "#" + entity.getDate(), entity);
        }

        @Override
        public void saveAll(List<HeatForecastDayEntity> entities) {
            entities.forEach(this::save);
        }

        @Override
        public List<HeatForecastDayEntity> findByLocationId(String locationId) {
            return storage.values().stream()
                    .filter(e -> e.getLocationId().equalsIgnoreCase(locationId))
                    .sorted(Comparator.comparing(HeatForecastDayEntity::getDate))
                    .toList();
        }

        @Override
        public Optional<HeatForecastDayEntity> findByLocationIdAndDate(String locationId, String date) {
            return Optional.ofNullable(storage.get(locationId + "#" + date));
        }
    }

    @Repository
    @ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
    public static class InMemoryHeatFeedStateRepository implements HeatFeedStateRepository {
        private final Map<String, HeatFeedStateEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void save(HeatFeedStateEntity entity) {
            storage.put(entity.getFeedName(), entity);
        }

        @Override
        public Optional<HeatFeedStateEntity> findByName(String feedName) {
            return Optional.ofNullable(storage.get(feedName));
        }
    }
}

