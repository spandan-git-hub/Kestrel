package com.kestrel.rulesengine.infra.repository;

import com.kestrel.rulesengine.infra.entity.AlertStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertStateRepository extends JpaRepository<AlertStateEntity, String> {
}
