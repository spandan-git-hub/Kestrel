package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.MonitoredLocationReplicaEntity;

import java.util.List;
import java.util.Optional;

public interface MonitoredLocationReplicaRepository {

    List<MonitoredLocationReplicaEntity> findByActiveTrue();

    Optional<MonitoredLocationReplicaEntity> findById(String locationId);

    MonitoredLocationReplicaEntity save(MonitoredLocationReplicaEntity entity);

    List<MonitoredLocationReplicaEntity> findAll();

    long count();

    default List<MonitoredLocationReplicaEntity> saveAll(Iterable<MonitoredLocationReplicaEntity> entities) {
        List<MonitoredLocationReplicaEntity> list = new java.util.ArrayList<>();
        for (MonitoredLocationReplicaEntity e : entities) {
            list.add(save(e));
        }
        return list;
    }
}
