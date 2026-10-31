package com.kestrel.aqingest.infra.repository;

import com.kestrel.aqingest.infra.entity.AqFeedStateEntity;

import java.util.Optional;

public interface AqFeedStateRepository {

    void save(AqFeedStateEntity entity);

    Optional<AqFeedStateEntity> findByName(String feedName);
}

