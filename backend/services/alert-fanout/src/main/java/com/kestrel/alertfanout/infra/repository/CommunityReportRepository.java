package com.kestrel.alertfanout.infra.repository;

import com.kestrel.alertfanout.infra.entity.CommunityReport;

import java.util.List;

public interface CommunityReportRepository {

    List<CommunityReport> findByLocationIdOrderByCreatedAtDesc(String locationId);

    CommunityReport save(CommunityReport report);

    List<CommunityReport> findAll();

    long count();

    void deleteAll();

    default List<CommunityReport> findTop50ByOrderByCreatedAtDesc() {
        return findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(50)
                .toList();
    }
}
