package com.kestrel.subscriberregistry.infra.repository.inmemory;

import com.kestrel.subscriberregistry.infra.entity.ConsentLogEntity;
import com.kestrel.subscriberregistry.infra.repository.ConsentLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "kestrel.persistence.type", havingValue = "inmemory")
public class InMemoryConsentLogRepository implements ConsentLogRepository {

    private final Map<String, ConsentLogEntity> store = new ConcurrentHashMap<>();

    @Override
    public ConsentLogEntity save(ConsentLogEntity consentLog) {
        store.put(consentLog.getConsentId(), consentLog);
        return consentLog;
    }

    @Override
    public Optional<ConsentLogEntity> findById(String consentId) {
        return Optional.ofNullable(store.get(consentId));
    }

    @Override
    public List<ConsentLogEntity> findByToken(String token) {
        return store.values().stream()
                .filter(c -> token.equals(c.getToken()))
                .toList();
    }

    @Override
    public List<ConsentLogEntity> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }
}

