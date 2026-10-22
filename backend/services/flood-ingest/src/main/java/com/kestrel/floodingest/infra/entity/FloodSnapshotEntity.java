package com.kestrel.floodingest.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "flood_snapshots",
    indexes = {
        @Index(name = "idx_snapshot_location_id", columnList = "location_id")
    }
)
public class FloodSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(name = "data_as_of", nullable = false)
    private Instant dataAsOf;

    @Column(name = "is_simulated", nullable = false)
    private boolean simulated;

    @Column(name = "scenario_name", length = 32)
    private String scenarioName;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Lob
    @Column(name = "days_json", nullable = false, length = 65535)
    private String daysJson;

    @Column(name = "is_stale", nullable = false)
    private boolean stale;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FloodSnapshotEntity() {}

    public FloodSnapshotEntity(
            String locationId,
            Instant dataAsOf,
            boolean simulated,
            String scenarioName,
            int memberCount,
            String daysJson,
            boolean stale) {
        this.locationId = locationId;
        this.dataAsOf = dataAsOf;
        this.simulated = simulated;
        this.scenarioName = scenarioName;
        this.memberCount = memberCount;
        this.daysJson = daysJson;
        this.stale = stale;
        this.createdAt = Instant.now();
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

    public Instant getDataAsOf() {
        return dataAsOf;
    }

    public void setDataAsOf(Instant dataAsOf) {
        this.dataAsOf = dataAsOf;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getDaysJson() {
        return daysJson;
    }

    public void setDaysJson(String daysJson) {
        this.daysJson = daysJson;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
