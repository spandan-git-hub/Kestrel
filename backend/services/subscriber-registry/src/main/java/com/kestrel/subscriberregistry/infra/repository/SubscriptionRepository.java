package com.kestrel.subscriberregistry.infra.repository;

import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

    List<SubscriptionEntity> findAll();

    Optional<SubscriptionEntity> findById(String token);

    List<SubscriptionEntity> findByLocationId(String locationId);

    List<SubscriptionEntity> findByLocationIdAndStatus(String locationId, String status);

    SubscriptionEntity save(SubscriptionEntity subscription);

    void delete(SubscriptionEntity subscription);

    void deleteById(String token);

    boolean existsById(String token);

    long count();
}
