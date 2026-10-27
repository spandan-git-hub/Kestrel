package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.ReportCategory;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.UUID;

@DynamoDbBean
public class CommunityReport {

    private Long id;
    private String reportId;
    private String locationId;
    private ReportCategory category;
    private String description;
    private String reporterAlias = "Resident";
    private Instant createdAt;
    private boolean provenanceUpgraded = true;
    private Long ttl;

    public CommunityReport() {}

    public CommunityReport(String locationId, ReportCategory category, String description, String reporterAlias) {
        this.reportId = UUID.randomUUID().toString();
        this.locationId = locationId;
        this.category = category;
        this.description = description;
        this.reporterAlias = reporterAlias != null && !reporterAlias.isBlank() ? reporterAlias : "Resident";
        this.createdAt = Instant.now();
        this.provenanceUpgraded = true;
        this.ttl = this.createdAt.getEpochSecond() + (30L * 24 * 3600);
        this.id = Math.abs((long) this.reportId.hashCode());
    }

    public Long getId() {
        if (id == null && reportId != null) {
            id = Math.abs((long) reportId.hashCode());
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @DynamoDbPartitionKey
    public String getReportId() {
        if (reportId == null) {
            reportId = UUID.randomUUID().toString();
        }
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "locationId-index")
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

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
