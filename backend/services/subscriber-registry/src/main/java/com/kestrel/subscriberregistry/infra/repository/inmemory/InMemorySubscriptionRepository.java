package com.kestrel.subscriberregistry.infra.repository.inmemory;

import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;
import com.kestrel.subscriberregistry.infra.repository.SubscriptionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<String, SubscriptionEntity> store = new ConcurrentHashMap<>();

    @Override
    public List<SubscriptionEntity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<SubscriptionEntity> findById(String token) {
        return Optional.ofNullable(store.get(token));
    }

    @Override
    public List<SubscriptionEntity> findByLocationId(String locationId) {
        return findByLocationIdAndStatus(locationId, null);
    }

    @Override
    public List<SubscriptionEntity> findByLocationIdAndStatus(String locationId, String status) {
        return store.values().stream()
                .filter(sub -> locationId.equals(sub.getLocationId()))
                .filter(sub -> status == null || status.equalsIgnoreCase(sub.getStatus()))
                .toList();
    }

    @Override
    public SubscriptionEntity save(SubscriptionEntity subscription) {
        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(Instant.now());
        }
        subscription.setUpdatedAt(Instant.now());
        store.put(subscription.getToken(), subscription);
        return subscription;
    }

    @Override
    public void delete(SubscriptionEntity subscription) {
        if (subscription != null && subscription.getToken() != null) {
            deleteById(subscription.getToken());
        }
    }

    @Override
    public void deleteById(String token) {
        store.remove(token);
    }

    @Override
    public boolean existsById(String token) {
        return store.containsKey(token);
    }

    @Override
    public long count() {
        return store.size();
    }
}
