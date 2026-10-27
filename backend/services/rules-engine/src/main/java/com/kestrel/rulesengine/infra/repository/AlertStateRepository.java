package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.AlertStateEntity;

import java.util.List;
import java.util.Optional;

public interface AlertStateRepository {

    Optional<AlertStateEntity> findById(String locationId);

    AlertStateEntity save(AlertStateEntity entity);

    List<AlertStateEntity> findAll();

    long count();

    default boolean existsById(String locationId) {
        return findById(locationId).isPresent();
    }
}
