package com.kestrel.heatingest.infra.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class HeatForecastDayEntity {

    private String locationId;
    private String date; // YYYY-MM-DD
    private double tmax;
    private double apparentMax;
    private double humidityMax;
    private double normalTmax;
    private double departure;
    private Long ttl;
    private Instant updatedAt;

    public HeatForecastDayEntity() {}

    public HeatForecastDayEntity(String locationId, String date, double tmax, double apparentMax, double humidityMax, double normalTmax, double departure) {
        this.locationId = locationId;
        this.date = date;
        this.tmax = tmax;
        this.apparentMax = apparentMax;
        this.humidityMax = humidityMax;
        this.normalTmax = normalTmax;
        this.departure = departure;
        this.updatedAt = Instant.now();
        // 30 days TTL from now
        this.ttl = Instant.now().getEpochSecond() + (30L * 24 * 3600);
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
    @DynamoDbAttribute("date")
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getTmax() {
        return tmax;
    }

    public void setTmax(double tmax) {
        this.tmax = tmax;
    }

    public double getApparentMax() {
        return apparentMax;
    }

    public void setApparentMax(double apparentMax) {
        this.apparentMax = apparentMax;
    }

    public double getHumidityMax() {
        return humidityMax;
    }

    public void setHumidityMax(double humidityMax) {
        this.humidityMax = humidityMax;
    }

    public double getNormalTmax() {
        return normalTmax;
    }

    public void setNormalTmax(double normalTmax) {
        this.normalTmax = normalTmax;
    }

    public double getDeparture() {
        return departure;
    }

    public void setDeparture(double departure) {
        this.departure = departure;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

