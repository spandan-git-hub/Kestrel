package com.kestrel.aqingest.infra.repository;

import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;

import java.util.List;

public interface AqHourlyValueRepository {

    void save(AqHourlyValueEntity entity);

    void saveAll(List<AqHourlyValueEntity> entities);

    List<AqHourlyValueEntity> findByLocationId(String locationId);

    List<AqHourlyValueEntity> findByLocationIdAndTimeRange(String locationId, String startTime, String endTime);
}

