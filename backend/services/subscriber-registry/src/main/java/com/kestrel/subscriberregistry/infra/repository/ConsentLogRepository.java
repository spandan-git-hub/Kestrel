package com.kestrel.subscriberregistry.infra.repository;

import com.kestrel.subscriberregistry.infra.entity.ConsentLogEntity;

import java.util.List;
import java.util.Optional;

public interface ConsentLogRepository {

    ConsentLogEntity save(ConsentLogEntity consentLog);

    Optional<ConsentLogEntity> findById(String consentId);

    List<ConsentLogEntity> findByToken(String token);

    List<ConsentLogEntity> findAll();

    long count();
}

