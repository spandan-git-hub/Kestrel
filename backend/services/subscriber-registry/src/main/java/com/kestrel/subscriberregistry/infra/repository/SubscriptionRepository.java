package com.kestrel.subscriberregistry.infra.repository;

import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {

    List<SubscriptionEntity> findByLocationId(String locationId);

    List<SubscriptionEntity> findByLocationIdAndStatus(String locationId, String status);
}
