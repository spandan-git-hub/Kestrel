package com.kestrel.repository;

import com.kestrel.domain.AlertStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertStateRepository extends JpaRepository<AlertStateEntity, String> {
}
