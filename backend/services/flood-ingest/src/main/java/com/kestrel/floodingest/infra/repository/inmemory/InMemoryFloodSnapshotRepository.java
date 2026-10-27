package com.kestrel.floodingest.infra.repository.inmemory;

import com.kestrel.floodingest.infra.entity.FloodSnapshotEntity;
import com.kestrel.floodingest.infra.repository.FloodSnapshotRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryFloodSnapshotRepository implements FloodSnapshotRepository {

    private final Map<Long, FloodSnapshotEntity> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<FloodSnapshotEntity> findFirstByLocationIdOrderByDataAsOfDesc(String locationId) {
        return store.values().stream()
                .filter(s -> locationId.equals(s.getLocationId()))
                .max(Comparator.comparing(FloodSnapshotEntity::getDataAsOf));
    }

    @Override
    public FloodSnapshotEntity save(FloodSnapshotEntity entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public List<FloodSnapshotEntity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<FloodSnapshotEntity> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long count() {
        return store.size();
    }
}
