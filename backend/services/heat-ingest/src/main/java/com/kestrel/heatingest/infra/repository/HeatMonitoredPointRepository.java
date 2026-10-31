package com.kestrel.heatingest.infra.repository;

import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;

import java.util.List;
import java.util.Optional;

public interface HeatMonitoredPointRepository {

    List<HeatMonitoredPointEntity> findAll();

    List<HeatMonitoredPointEntity> findByActiveTrue();

    Optional<HeatMonitoredPointEntity> findById(String locationId);

    void save(HeatMonitoredPointEntity entity);

    void saveAll(List<HeatMonitoredPointEntity> entities);

    boolean existsById(String locationId);

    long count();
}

