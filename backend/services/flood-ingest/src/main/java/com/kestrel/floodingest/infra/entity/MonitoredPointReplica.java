package com.kestrel.floodingest.infra.entity;

import com.kestrel.common.events.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "monitored_points_replica")
public class MonitoredPointReplica {

    @Id
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String state;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "river_name", nullable = false, length = 128)
    private String riverName;

    @Column(name = "threshold_2yr", nullable = false)
    private double threshold2yr;

    @Column(name = "threshold_5yr", nullable = false)
    private double threshold5yr;

    @Column(name = "threshold_20yr", nullable = false)
    private double threshold20yr;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_language", nullable = false, length = 16)
    private Language primaryLanguage;

    @Column(nullable = false)
    private boolean active;

    public MonitoredPointReplica() {}

    public MonitoredPointReplica(
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
        this.locationId = locationId;
        this.name = name;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.riverName = riverName;
        this.threshold2yr = threshold2yr;
        this.threshold5yr = threshold5yr;
        this.threshold20yr = threshold20yr;
        this.primaryLanguage = primaryLanguage;
        this.active = active;
    }

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
}
