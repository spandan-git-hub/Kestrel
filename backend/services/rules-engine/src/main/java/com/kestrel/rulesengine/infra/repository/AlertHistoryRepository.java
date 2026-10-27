package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.AlertHistoryEntity;

import java.util.List;

public interface AlertHistoryRepository {

    List<AlertHistoryEntity> findByLocationIdOrderByTransitionedAtDesc(String locationId);

    default List<AlertHistoryEntity> findByLocationIdOrderByTransitionAtDesc(String locationId) {
        return findByLocationIdOrderByTransitionedAtDesc(locationId);
    }

    AlertHistoryEntity save(AlertHistoryEntity entity);

    List<AlertHistoryEntity> findAll();

    long count();
}
