package com.kestrel.floodingest.infra.repository;

import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;

import java.util.List;
import java.util.Optional;

public interface MonitoredPointReplicaRepository {

    List<MonitoredPointReplica> findByActiveTrue();

    Optional<MonitoredPointReplica> findById(String locationId);

    MonitoredPointReplica save(MonitoredPointReplica point);

    default List<MonitoredPointReplica> saveAll(Iterable<MonitoredPointReplica> points) {
        List<MonitoredPointReplica> result = new java.util.ArrayList<>();
        for (MonitoredPointReplica p : points) {
            result.add(save(p));
        }
        return result;
    }

    List<MonitoredPointReplica> findAll();

    boolean existsById(String locationId);

    long count();
}
