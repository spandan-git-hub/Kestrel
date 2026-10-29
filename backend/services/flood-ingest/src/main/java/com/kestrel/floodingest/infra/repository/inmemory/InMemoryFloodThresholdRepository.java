package com.kestrel.floodingest.infra.repository.inmemory;

import com.kestrel.floodingest.infra.entity.FloodThresholdEntity;
import com.kestrel.floodingest.infra.repository.FloodThresholdRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryFloodThresholdRepository implements FloodThresholdRepository {

    private final Map<String, FloodThresholdEntity> store = new ConcurrentHashMap<>();

    @Override
    public Optional<FloodThresholdEntity> findById(String locationId) {
        return Optional.ofNullable(store.get(locationId));
    }

    @Override
    public FloodThresholdEntity save(FloodThresholdEntity entity) {
        store.put(entity.getLocationId(), entity);
        return entity;
    }

    @Override
    public List<FloodThresholdEntity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void saveAll(Iterable<FloodThresholdEntity> entities) {
        entities.forEach(this::save);
    }

    @Override
    public boolean existsById(String locationId) {
        return store.containsKey(locationId);
    }

    @Override
    public long count() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}

