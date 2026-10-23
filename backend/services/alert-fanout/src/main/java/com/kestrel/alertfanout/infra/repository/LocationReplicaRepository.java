package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.LocationReplica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationReplicaRepository extends JpaRepository<LocationReplica, String> {
    List<LocationReplica> findByActiveTrue();
}
