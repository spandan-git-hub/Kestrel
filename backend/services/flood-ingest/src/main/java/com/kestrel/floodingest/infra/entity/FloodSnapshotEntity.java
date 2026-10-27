package com.kestrel.floodingest.infra.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class FloodSnapshotEntity {

    private Long id;
    private String locationId;
    private String snapshotTimestamp;
    private Instant dataAsOf;
    private boolean simulated;
    private String scenarioName;
    private int memberCount;
    private String daysJson;
    private boolean stale;
    private Instant createdAt;
    private Long ttl;

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
        this.snapshotTimestamp = (dataAsOf != null) ? dataAsOf.toString() : Instant.now().toString();
        this.simulated = simulated;
        this.scenarioName = scenarioName;
        this.memberCount = memberCount;
        this.daysJson = daysJson;
        this.stale = stale;
        this.createdAt = Instant.now();
        this.ttl = (dataAsOf != null ? dataAsOf.getEpochSecond() : Instant.now().getEpochSecond()) + (30L * 24 * 3600);
        this.id = Math.abs((long) (locationId + "#" + snapshotTimestamp).hashCode());
    }

    public Long getId() {
        if (id == null && locationId != null && snapshotTimestamp != null) {
            id = Math.abs((long) (locationId + "#" + snapshotTimestamp).hashCode());
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @DynamoDbSortKey
    public String getSnapshotTimestamp() {
        if (snapshotTimestamp == null && dataAsOf != null) {
            snapshotTimestamp = dataAsOf.toString();
        }
        return snapshotTimestamp;
    }

    public void setSnapshotTimestamp(String snapshotTimestamp) {
        this.snapshotTimestamp = snapshotTimestamp;
    }

    public Instant getDataAsOf() {
        return dataAsOf;
    }

    public void setDataAsOf(Instant dataAsOf) {
        this.dataAsOf = dataAsOf;
        if (this.snapshotTimestamp == null && dataAsOf != null) {
            this.snapshotTimestamp = dataAsOf.toString();
        }
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

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
