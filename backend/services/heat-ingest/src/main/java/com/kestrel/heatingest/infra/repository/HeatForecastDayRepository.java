package com.kestrel.heatingest.infra.repository;

import com.kestrel.heatingest.infra.entity.HeatForecastDayEntity;

import java.util.List;
import java.util.Optional;

public interface HeatForecastDayRepository {

    void save(HeatForecastDayEntity entity);

    void saveAll(List<HeatForecastDayEntity> entities);

    List<HeatForecastDayEntity> findByLocationId(String locationId);

    Optional<HeatForecastDayEntity> findByLocationIdAndDate(String locationId, String date);
}

