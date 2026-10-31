package com.kestrel.heatingest.infra.repository;

import com.kestrel.heatingest.infra.entity.HeatForecastRunEntity;

import java.util.List;
import java.util.Optional;

public interface HeatForecastRunRepository {

    void save(HeatForecastRunEntity entity);

    Optional<HeatForecastRunEntity> findLatestByLocationId(String locationId);

    List<HeatForecastRunEntity> findByLocationId(String locationId);
}

