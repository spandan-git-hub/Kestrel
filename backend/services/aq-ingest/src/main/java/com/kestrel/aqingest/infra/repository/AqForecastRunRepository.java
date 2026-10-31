package com.kestrel.aqingest.infra.repository;

import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;

import java.util.List;
import java.util.Optional;

public interface AqForecastRunRepository {

    void save(AqForecastRunEntity entity);

    Optional<AqForecastRunEntity> findLatestByLocationId(String locationId);

    List<AqForecastRunEntity> findByLocationId(String locationId);
}

