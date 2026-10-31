package com.kestrel.heatingest.infra.repository;

import com.kestrel.heatingest.infra.entity.HeatNormalEntity;

import java.util.List;
import java.util.Optional;

public interface HeatNormalRepository {

    Optional<HeatNormalEntity> findByLocationIdAndDayOfYear(String locationId, int dayOfYear);

    List<HeatNormalEntity> findByLocationId(String locationId);

    void save(HeatNormalEntity entity);

    void saveAll(List<HeatNormalEntity> entities);

    long countByLocationId(String locationId);

    long count();
}

