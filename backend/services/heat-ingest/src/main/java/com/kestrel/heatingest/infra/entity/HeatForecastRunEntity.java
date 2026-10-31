package com.kestrel.heatingest.infra.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class HeatForecastRunEntity {

    private String locationId;
    private String runId;
    private Instant fetchedAt;
    private Instant dataAsOf;
    private int daysCount;
    private double peakDeparture;
    private Long ttl;
    private Instant createdAt;

    public HeatForecastRunEntity() {}

    public HeatForecastRunEntity(String locationId, String runId, Instant fetchedAt, Instant dataAsOf, int daysCount, double peakDeparture) {
        this.locationId = locationId;
        this.runId = runId;
        this.fetchedAt = fetchedAt;
        this.dataAsOf = dataAsOf;
        this.daysCount = daysCount;
        this.peakDeparture = peakDeparture;
        this.createdAt = Instant.now();
        // 30 days TTL
        this.ttl = (dataAsOf != null ? dataAsOf.getEpochSecond() : Instant.now().getEpochSecond()) + (30L * 24 * 3600);
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("locationId")
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("runId")
    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Instant getDataAsOf() {
        return dataAsOf;
    }

    public void setDataAsOf(Instant dataAsOf) {
        this.dataAsOf = dataAsOf;
    }

    public int getDaysCount() {
        return daysCount;
    }

    public void setDaysCount(int daysCount) {
        this.daysCount = daysCount;
    }

    public double getPeakDeparture() {
        return peakDeparture;
    }

    public void setPeakDeparture(double peakDeparture) {
        this.peakDeparture = peakDeparture;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

