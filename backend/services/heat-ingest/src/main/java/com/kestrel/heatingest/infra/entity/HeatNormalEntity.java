package com.kestrel.heatingest.infra.entity;

import com.kestrel.common.events.RegionType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class HeatNormalEntity {

    private String locationId;
    private Integer dayOfYear;
    private double normalTmax;
    private RegionType regionType;
    private boolean smoothed;
    private Instant updatedAt;

    public HeatNormalEntity() {}

    public HeatNormalEntity(String locationId, Integer dayOfYear, double normalTmax, RegionType regionType, boolean smoothed) {
        this.locationId = locationId;
        this.dayOfYear = dayOfYear;
        this.normalTmax = normalTmax;
        this.regionType = regionType;
        this.smoothed = smoothed;
        this.updatedAt = Instant.now();
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
    @DynamoDbAttribute("dayOfYear")
    public Integer getDayOfYear() {
        return dayOfYear;
    }

    public void setDayOfYear(Integer dayOfYear) {
        this.dayOfYear = dayOfYear;
    }

    public double getNormalTmax() {
        return normalTmax;
    }

    public void setNormalTmax(double normalTmax) {
        this.normalTmax = normalTmax;
    }

    public RegionType getRegionType() {
        return regionType;
    }

    public void setRegionType(RegionType regionType) {
        this.regionType = regionType;
    }

    public boolean isSmoothed() {
        return smoothed;
    }

    public void setSmoothed(boolean smoothed) {
        this.smoothed = smoothed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

