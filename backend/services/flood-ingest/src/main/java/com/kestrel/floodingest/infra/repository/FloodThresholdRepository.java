package com.kestrel.floodingest.infra.repository;

import com.kestrel.floodingest.infra.entity.FloodThresholdEntity;

import java.util.List;
import java.util.Optional;

public interface FloodThresholdRepository {

    Optional<FloodThresholdEntity> findById(String locationId);

    FloodThresholdEntity save(FloodThresholdEntity entity);

    List<FloodThresholdEntity> findAll();

    void saveAll(Iterable<FloodThresholdEntity> entities);

    boolean existsById(String locationId);

    long count();
}

