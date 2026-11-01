package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.RegionType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@DynamoDbBean
public class LocationReplica {

    private String locationId;
    private String name;
    private String state;
    private RegionType regionType;
    private double latitude;
    private double longitude;
    private String riverName;
    private double threshold2yr;
    private double threshold5yr;
    private double threshold20yr;
    private Language primaryLanguage = Language.en;
    private boolean active = true;
    private Provenance currentProvenance = Provenance.MODEL_ONLY;
    private int reportCount = 0;
    private Instant lastReportedAt;
    private Instant updatedAt;

    public LocationReplica() {
    }

    public LocationReplica(
            String locationId,
            String name,
            String state,
            RegionType regionType,
            double latitude,
            double longitude,
            String riverName,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            Language primaryLanguage,
            boolean active) {
        this.locationId = locationId;
        this.name = name;
        this.state = state;
        this.regionType = regionType != null ? regionType : (locationId != null ? IndianRiverCatalog.getRegionType(locationId) : RegionType.PLAINS);
        this.latitude = latitude;
        this.longitude = longitude;
        this.riverName = riverName;
        this.threshold2yr = threshold2yr;
        this.threshold5yr = threshold5yr;
        this.threshold20yr = threshold20yr;
        this.primaryLanguage = primaryLanguage;
        this.active = active;
        this.currentProvenance = Provenance.MODEL_ONLY;
        this.reportCount = 0;
        this.updatedAt = Instant.now();
    }

    public LocationReplica(
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
        this(locationId, name, state, IndianRiverCatalog.getRegionType(locationId), latitude, longitude, riverName, threshold2yr, threshold5yr, threshold20yr, primaryLanguage, active);
    }

    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
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

    public String getRiverName() {
        return riverName;
    }

    public void setRiverName(String riverName) {
        this.riverName = riverName;
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

    public Provenance getCurrentProvenance() {
        return currentProvenance;
    }

    public void setCurrentProvenance(Provenance currentProvenance) {
        this.currentProvenance = currentProvenance;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public Instant getLastReportedAt() {
        return lastReportedAt;
    }

    public void setLastReportedAt(Instant lastReportedAt) {
        this.lastReportedAt = lastReportedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
