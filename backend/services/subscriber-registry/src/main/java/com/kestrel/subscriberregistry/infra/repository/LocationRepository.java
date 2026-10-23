package com.kestrel.subscriberregistry.infra.repository;

import com.kestrel.subscriberregistry.infra.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntity, String> {

    List<LocationEntity> findByActiveTrue();
}
