package com.kestrel.heatingest.infra.repository;

import com.kestrel.heatingest.infra.entity.HeatFeedStateEntity;

import java.util.Optional;

public interface HeatFeedStateRepository {

    void save(HeatFeedStateEntity entity);

    Optional<HeatFeedStateEntity> findByName(String feedName);
}

