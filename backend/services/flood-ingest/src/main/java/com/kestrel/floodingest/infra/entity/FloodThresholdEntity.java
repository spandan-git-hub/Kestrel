package com.kestrel.floodingest.infra.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@DynamoDbBean
public class FloodThresholdEntity {

    private String locationId;
    private String riverName;
    private String basin;
    private double latitude;
    private double longitude;
    private double mean;
    private double stdDev;
    private double locationMu;
    private double scaleBeta;
    private double threshold2yr;
    private double threshold5yr;
    private double threshold20yr;
    private int sampleCount;
    private Instant fittedAt;

    public FloodThresholdEntity() {}

    public FloodThresholdEntity(
            String locationId,
            String riverName,
            String basin,
            double latitude,
            double longitude,
            double mean,
            double stdDev,
            double locationMu,
            double scaleBeta,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            int sampleCount,
            Instant fittedAt) {
        this.locationId = locationId;
        this.riverName = riverName;
        this.basin = basin;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mean = mean;
        this.stdDev = stdDev;
        this.locationMu = locationMu;
        this.scaleBeta = scaleBeta;
        this.threshold2yr = threshold2yr;
        this.threshold5yr = threshold5yr;
        this.threshold20yr = threshold20yr;
        this.sampleCount = sampleCount;
        this.fittedAt = fittedAt;
    }

    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
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

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStdDev() {
        return stdDev;
    }

    public void setStdDev(double stdDev) {
        this.stdDev = stdDev;
    }

    public double getLocationMu() {
        return locationMu;
    }

    public void setLocationMu(double locationMu) {
        this.locationMu = locationMu;
    }

    public double getScaleBeta() {
        return scaleBeta;
    }

    public void setScaleBeta(double scaleBeta) {
        this.scaleBeta = scaleBeta;
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

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Instant getFittedAt() {
        return fittedAt;
    }

    public void setFittedAt(Instant fittedAt) {
        this.fittedAt = fittedAt;
    }
}

