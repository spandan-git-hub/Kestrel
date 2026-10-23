package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.CommunityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {
    List<CommunityReport> findByLocationIdOrderByCreatedAtDesc(String locationId);
    List<CommunityReport> findTop50ByOrderByCreatedAtDesc();
    long countByLocationId(String locationId);
}
