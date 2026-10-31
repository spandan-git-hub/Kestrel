package com.kestrel.aqingest.infra.repository;

import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;

import java.util.List;
import java.util.Optional;

public interface AqMonitoredPointRepository {

    List<AqMonitoredPointEntity> findAll();

    List<AqMonitoredPointEntity> findByActiveTrue();

    Optional<AqMonitoredPointEntity> findById(String locationId);

    void save(AqMonitoredPointEntity entity);

    void saveAll(List<AqMonitoredPointEntity> entities);

    boolean existsById(String locationId);

    long count();
}

