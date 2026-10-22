package com.kestrel.repository;

import com.kestrel.domain.DeliveryLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLogEntity, Long> {
    List<DeliveryLogEntity> findTop50ByOrderBySentAtDesc();
}
