package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.AlertHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistoryEntity, Long> {
    List<AlertHistoryEntity> findByLocationIdOrderByTransitionedAtDesc(String locationId);
}
