package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.ActiveAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActiveAlertRepository extends JpaRepository<ActiveAlertEntity, String> {
    Optional<ActiveAlertEntity> findByLocationId(String locationId);
}
