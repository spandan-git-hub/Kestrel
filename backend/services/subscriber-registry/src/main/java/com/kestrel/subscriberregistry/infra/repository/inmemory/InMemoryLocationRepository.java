package com.kestrel.subscriberregistry.infra.repository.inmemory;

import com.kestrel.subscriberregistry.infra.entity.LocationEntity;
import com.kestrel.subscriberregistry.infra.repository.LocationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryLocationRepository implements LocationRepository {

    private final Map<String, LocationEntity> store = new ConcurrentHashMap<>();

    @Override
    public List<LocationEntity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<LocationEntity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public LocationEntity save(LocationEntity location) {
        if (location.getCreatedAt() == null) {
            location.setCreatedAt(Instant.now());
        }
        location.setUpdatedAt(Instant.now());
        store.put(location.getId(), location);
        return location;
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public List<LocationEntity> findByActiveTrue() {
        return store.values().stream()
                .filter(LocationEntity::isActive)
                .toList();
    }

    @Override
    public long count() {
        return store.size();
    }
}
