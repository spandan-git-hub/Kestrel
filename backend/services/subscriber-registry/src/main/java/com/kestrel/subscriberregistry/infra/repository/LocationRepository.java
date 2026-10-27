package com.kestrel.subscriberregistry.infra.repository;

import com.kestrel.subscriberregistry.infra.entity.LocationEntity;

import java.util.List;
import java.util.Optional;

public interface LocationRepository {

    List<LocationEntity> findAll();

    Optional<LocationEntity> findById(String id);

    LocationEntity save(LocationEntity location);

    boolean existsById(String id);

    void deleteById(String id);

    List<LocationEntity> findByActiveTrue();

    long count();

    default List<LocationEntity> saveAll(Iterable<LocationEntity> entities) {
        List<LocationEntity> result = new java.util.ArrayList<>();
        for (LocationEntity entity : entities) {
            result.add(save(entity));
        }
        return result;
    }
}
