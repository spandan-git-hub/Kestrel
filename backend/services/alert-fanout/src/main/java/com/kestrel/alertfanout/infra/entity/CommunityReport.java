package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.ReportCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "community_reports")
public class CommunityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportCategory category;

    @Column(length = 1024)
    private String description;

    @Column(name = "reporter_alias", length = 64)
    private String reporterAlias = "Resident";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "provenance_upgraded", nullable = false)
    private boolean provenanceUpgraded = true;

    public CommunityReport() {}

    public CommunityReport(String locationId, ReportCategory category, String description, String reporterAlias) {
        this.locationId = locationId;
        this.category = category;
        this.description = description;
        this.reporterAlias = reporterAlias != null && !reporterAlias.isBlank() ? reporterAlias : "Resident";
        this.createdAt = Instant.now();
        this.provenanceUpgraded = true;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public ReportCategory getCategory() {
        return category;
    }

    public void setCategory(ReportCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReporterAlias() {
        return reporterAlias;
    }

    public void setReporterAlias(String reporterAlias) {
        this.reporterAlias = reporterAlias;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isProvenanceUpgraded() {
        return provenanceUpgraded;
    }

    public void setProvenanceUpgraded(boolean provenanceUpgraded) {
        this.provenanceUpgraded = provenanceUpgraded;
    }
}
