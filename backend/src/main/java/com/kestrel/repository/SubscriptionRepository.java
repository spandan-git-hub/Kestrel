package com.kestrel.repository;

import com.kestrel.domain.SubscriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByToken(String token);
    List<SubscriptionEntity> findByLocationIdAndActiveTrue(String locationId);
}
