package com.kestrel.floodingest.infra.repository;

import com.kestrel.floodingest.infra.entity.FloodSnapshotEntity;

import java.util.List;
import java.util.Optional;

public interface FloodSnapshotRepository {

    Optional<FloodSnapshotEntity> findFirstByLocationIdOrderByDataAsOfDesc(String locationId);

    FloodSnapshotEntity save(FloodSnapshotEntity entity);

    List<FloodSnapshotEntity> findAll();

    Optional<FloodSnapshotEntity> findById(Long id);

    long count();
}
