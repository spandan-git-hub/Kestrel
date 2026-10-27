package com.kestrel.rulesengine.infra.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@DynamoDbBean
public class ForecastSnapshotReplicaEntity {

    private String locationId;
    private Instant dataAsOf;
    private boolean isSimulated;
    private int memberCount;
    private String forecastDaysJson;

    public ForecastSnapshotReplicaEntity() {}

    public ForecastSnapshotReplicaEntity(
            String locationId,
            Instant dataAsOf,
            boolean isSimulated,
            int memberCount,
            String forecastDaysJson) {
        this.locationId = locationId;
        this.dataAsOf = dataAsOf;
        this.isSimulated = isSimulated;
        this.memberCount = memberCount;
        this.forecastDaysJson = forecastDaysJson;
    }

    @DynamoDbPartitionKey
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
        return isSimulated;
    }

    public void setSimulated(boolean simulated) {
        isSimulated = simulated;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getForecastDaysJson() {
        return forecastDaysJson;
    }

    public void setForecastDaysJson(String forecastDaysJson) {
        this.forecastDaysJson = forecastDaysJson;
    }
}
