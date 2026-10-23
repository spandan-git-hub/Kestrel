package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.ForecastSnapshotReplicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForecastSnapshotReplicaRepository extends JpaRepository<ForecastSnapshotReplicaEntity, String> {
}
