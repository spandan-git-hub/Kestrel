package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;

import java.util.List;
import java.util.Optional;

public interface ActiveAlertRepository {

    Optional<ActiveAlertEntity> findById(String locationId);

    ActiveAlertEntity save(ActiveAlertEntity entity);

    void deleteById(String locationId);

    List<ActiveAlertEntity> findAll();

    long count();

    default void deleteAll() {
        findAll().forEach(item -> deleteById(item.getLocationId()));
    }

    default boolean existsById(String locationId) {
        return findById(locationId).isPresent();
    }

    default Optional<ActiveAlertEntity> findByLocationId(String locationId) {
        return findById(locationId);
    }
}
