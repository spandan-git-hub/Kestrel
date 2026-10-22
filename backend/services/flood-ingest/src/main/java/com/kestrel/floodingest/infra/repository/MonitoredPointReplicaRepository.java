package com.kestrel.floodingest.infra.repository;

import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoredPointReplicaRepository extends JpaRepository<MonitoredPointReplica, String> {
    List<MonitoredPointReplica> findByActiveTrue();
}
