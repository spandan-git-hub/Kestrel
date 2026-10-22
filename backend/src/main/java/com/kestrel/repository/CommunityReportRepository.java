package com.kestrel.repository;

import com.kestrel.domain.CommunityReportEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReportEntity, Long> {
    List<CommunityReportEntity> findByLocationIdOrderByCreatedAtDesc(String locationId);
    long countByLocationId(String locationId);
}
