package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.LocationReplica;

import java.util.List;
import java.util.Optional;

public interface LocationReplicaRepository {

    Optional<LocationReplica> findById(String locationId);

    LocationReplica save(LocationReplica replica);

    List<LocationReplica> findAll();

    long count();

    default List<LocationReplica> saveAll(Iterable<LocationReplica> replicas) {
        List<LocationReplica> result = new java.util.ArrayList<>();
        for (LocationReplica r : replicas) {
            result.add(save(r));
        }
        return result;
    }
}
