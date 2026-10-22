package com.kestrel.floodingest.infra.repository;

import com.kestrel.floodingest.infra.entity.FloodSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FloodSnapshotRepository extends JpaRepository<FloodSnapshotEntity, Long> {
    Optional<FloodSnapshotEntity> findFirstByLocationIdOrderByDataAsOfDesc(String locationId);
}
