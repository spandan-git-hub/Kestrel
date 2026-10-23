package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.MonitoredLocationReplicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoredLocationReplicaRepository extends JpaRepository<MonitoredLocationReplicaEntity, String> {
    List<MonitoredLocationReplicaEntity> findByActiveTrue();
}
