package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.ForecastSnapshotReplicaEntity;

import java.util.List;
import java.util.Optional;

public interface ForecastSnapshotReplicaRepository {

    Optional<ForecastSnapshotReplicaEntity> findFirstByLocationIdOrderByDataAsOfDesc(String locationId);

    Optional<ForecastSnapshotReplicaEntity> findById(String locationId);

    ForecastSnapshotReplicaEntity save(ForecastSnapshotReplicaEntity entity);

    List<ForecastSnapshotReplicaEntity> findAll();

    long count();

    default boolean existsById(String locationId) {
        return findById(locationId).isPresent();
    }
}
