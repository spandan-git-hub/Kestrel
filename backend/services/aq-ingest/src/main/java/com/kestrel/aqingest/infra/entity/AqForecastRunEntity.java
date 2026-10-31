package com.kestrel.aqingest.infra.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class AqForecastRunEntity {

    private String locationId;
    private String runId;
    private Instant fetchedAt;
    private Instant dataAsOf;
    private double currentPm25_24h;
    private double currentPm10_24h;
    private double currentNo2_24h;
    private double currentSo2_24h;
    private double currentO3_8h;
    private double currentCo_8h_mgm3;
    private int validHours;
    private int forecastDaysCount;
    private Long ttl;
    private Instant createdAt;

    public AqForecastRunEntity() {}

    public AqForecastRunEntity(
            String locationId,
            String runId,
            Instant fetchedAt,
            Instant dataAsOf,
            double currentPm25_24h,
            double currentPm10_24h,
            double currentNo2_24h,
            double currentSo2_24h,
            double currentO3_8h,
            double currentCo_8h_mgm3,
            int validHours,
            int forecastDaysCount) {
        this.locationId = locationId;
        this.runId = runId;
        this.fetchedAt = fetchedAt;
        this.dataAsOf = dataAsOf;
        this.currentPm25_24h = currentPm25_24h;
        this.currentPm10_24h = currentPm10_24h;
        this.currentNo2_24h = currentNo2_24h;
        this.currentSo2_24h = currentSo2_24h;
        this.currentO3_8h = currentO3_8h;
        this.currentCo_8h_mgm3 = currentCo_8h_mgm3;
        this.validHours = validHours;
        this.forecastDaysCount = forecastDaysCount;
        this.createdAt = Instant.now();
        // 5 days TTL
        this.ttl = (dataAsOf != null ? dataAsOf.getEpochSecond() : Instant.now().getEpochSecond()) + (5L * 24 * 3600);
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

    public double getCurrentPm25_24h() {
        return currentPm25_24h;
    }

    public void setCurrentPm25_24h(double currentPm25_24h) {
        this.currentPm25_24h = currentPm25_24h;
    }

    public double getCurrentPm10_24h() {
        return currentPm10_24h;
    }

    public void setCurrentPm10_24h(double currentPm10_24h) {
        this.currentPm10_24h = currentPm10_24h;
    }

    public double getCurrentNo2_24h() {
        return currentNo2_24h;
    }

    public void setCurrentNo2_24h(double currentNo2_24h) {
        this.currentNo2_24h = currentNo2_24h;
    }

    public double getCurrentSo2_24h() {
        return currentSo2_24h;
    }

    public void setCurrentSo2_24h(double currentSo2_24h) {
        this.currentSo2_24h = currentSo2_24h;
    }

    public double getCurrentO3_8h() {
        return currentO3_8h;
    }

    public void setCurrentO3_8h(double currentO3_8h) {
        this.currentO3_8h = currentO3_8h;
    }

    public double getCurrentCo_8h_mgm3() {
        return currentCo_8h_mgm3;
    }

    public void setCurrentCo_8h_mgm3(double currentCo_8h_mgm3) {
        this.currentCo_8h_mgm3 = currentCo_8h_mgm3;
    }

    public int getValidHours() {
        return validHours;
    }

    public void setValidHours(int validHours) {
        this.validHours = validHours;
    }

    public int getForecastDaysCount() {
        return forecastDaysCount;
    }

    public void setForecastDaysCount(int forecastDaysCount) {
        this.forecastDaysCount = forecastDaysCount;
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

