package com.kestrel.floodingest.infra.repository.inmemory;

import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import com.kestrel.floodingest.infra.repository.MonitoredPointReplicaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryMonitoredPointReplicaRepository implements MonitoredPointReplicaRepository {

    private final Map<String, MonitoredPointReplica> store = new ConcurrentHashMap<>();

    @Override
    public List<MonitoredPointReplica> findByActiveTrue() {
        return store.values().stream()
                .filter(MonitoredPointReplica::isActive)
                .toList();
    }

    @Override
    public Optional<MonitoredPointReplica> findById(String locationId) {
        return Optional.ofNullable(store.get(locationId));
    }

    @Override
    public MonitoredPointReplica save(MonitoredPointReplica point) {
        store.put(point.getLocationId(), point);
        return point;
    }

    @Override
    public List<MonitoredPointReplica> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean existsById(String locationId) {
        return store.containsKey(locationId);
    }

    @Override
    public long count() {
        return store.size();
    }
}
