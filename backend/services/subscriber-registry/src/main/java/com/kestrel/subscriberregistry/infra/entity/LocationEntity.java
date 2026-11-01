package com.kestrel.subscriberregistry.infra.entity;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.RegionType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@DynamoDbBean
public class LocationEntity {

    private String locationId;
    private String name;
    private String state;
    private String riverName;
    private String basin;
    private RegionType regionType;
    private double latitude;
    private double longitude;
    private double threshold2yr;
    private double threshold5yr;
    private double threshold20yr;
    private Language primaryLanguage;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    public LocationEntity() {
    }

    public LocationEntity(
            String locationId,
            String name,
            String state,
            String riverName,
            String basin,
            RegionType regionType,
            double latitude,
            double longitude,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            Language primaryLanguage,
            boolean active) {
        this.locationId = locationId;
        this.name = name;
        this.state = state;
        this.riverName = riverName;
        this.basin = basin;
        this.regionType = regionType != null ? regionType : (locationId != null ? IndianRiverCatalog.getRegionType(locationId) : RegionType.PLAINS);
        this.latitude = latitude;
        this.longitude = longitude;
        this.threshold2yr = threshold2yr;
        this.threshold5yr = threshold5yr;
        this.threshold20yr = threshold20yr;
        this.primaryLanguage = primaryLanguage;
        this.active = active;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public LocationEntity(
            String locationId,
            String name,
            String state,
            String riverName,
            String basin,
            double latitude,
            double longitude,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            Language primaryLanguage,
            boolean active) {
        this(locationId, name, state, riverName, basin, IndianRiverCatalog.getRegionType(locationId), latitude, longitude, threshold2yr, threshold5yr, threshold20yr, primaryLanguage, active);
    }

    public LocationEntity(
            String locationId,
            String name,
            String state,
            double latitude,
            double longitude,
            String riverName,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            Language primaryLanguage,
            boolean active) {
        this(locationId, name, state, riverName, "India", latitude, longitude, threshold2yr, threshold5yr, threshold20yr, primaryLanguage, active);
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("locationId")
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @DynamoDbIgnore
    public String getId() {
        return locationId;
    }

    public void setId(String id) {
        this.locationId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRiverName() {
        return riverName;
    }

    public void setRiverName(String riverName) {
        this.riverName = riverName;
    }

    public String getBasin() {
        return basin;
    }

    public void setBasin(String basin) {
        this.basin = basin;
    }

    @DynamoDbAttribute("regionType")
    public RegionType getRegionType() {
        if (regionType == null && locationId != null) {
            return IndianRiverCatalog.getRegionType(locationId);
        }
        return regionType;
    }

    public void setRegionType(RegionType regionType) {
        this.regionType = regionType;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getThreshold2yr() {
        return threshold2yr;
    }

    public void setThreshold2yr(double threshold2yr) {
        this.threshold2yr = threshold2yr;
    }

    public double getThreshold5yr() {
        return threshold5yr;
    }

    public void setThreshold5yr(double threshold5yr) {
        this.threshold5yr = threshold5yr;
    }

    public double getThreshold20yr() {
        return threshold20yr;
    }

    public void setThreshold20yr(double threshold20yr) {
        this.threshold20yr = threshold20yr;
    }

    public Language getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(Language primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
