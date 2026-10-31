package com.kestrel.aqingest.infra.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class AqHourlyValueEntity {

    private String locationId;
    private String time; // ISO-8601 string, e.g. 2026-10-30T00:00
    private Double pm25;
    private Double pm10;
    private Double no2;
    private Double so2;
    private Double ozone;
    private Double coMgm3;
    private Double pm25_24h;
    private Double pm10_24h;
    private Double no2_24h;
    private Double so2_24h;
    private Double o3_8h;
    private Double co_8h_mgm3;
    private Long ttl;
    private Instant updatedAt;

    public AqHourlyValueEntity() {}

    public AqHourlyValueEntity(
            String locationId,
            String time,
            Double pm25,
            Double pm10,
            Double no2,
            Double so2,
            Double ozone,
            Double coMgm3,
            Double pm25_24h,
            Double pm10_24h,
            Double no2_24h,
            Double so2_24h,
            Double o3_8h,
            Double co_8h_mgm3) {
        this.locationId = locationId;
        this.time = time;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.no2 = no2;
        this.so2 = so2;
        this.ozone = ozone;
        this.coMgm3 = coMgm3;
        this.pm25_24h = pm25_24h;
        this.pm10_24h = pm10_24h;
        this.no2_24h = no2_24h;
        this.so2_24h = so2_24h;
        this.o3_8h = o3_8h;
        this.co_8h_mgm3 = co_8h_mgm3;
        this.updatedAt = Instant.now();
        // 5 days TTL
        this.ttl = Instant.now().getEpochSecond() + (5L * 24 * 3600);
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
    @DynamoDbAttribute("time")
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Double getPm25() {
        return pm25;
    }

    public void setPm25(Double pm25) {
        this.pm25 = pm25;
    }

    public Double getPm10() {
        return pm10;
    }

    public void setPm10(Double pm10) {
        this.pm10 = pm10;
    }

    public Double getNo2() {
        return no2;
    }

    public void setNo2(Double no2) {
        this.no2 = no2;
    }

    public Double getSo2() {
        return so2;
    }

    public void setSo2(Double so2) {
        this.so2 = so2;
    }

    public Double getOzone() {
        return ozone;
    }

    public void setOzone(Double ozone) {
        this.ozone = ozone;
    }

    public Double getCoMgm3() {
        return coMgm3;
    }

    public void setCoMgm3(Double coMgm3) {
        this.coMgm3 = coMgm3;
    }

    public Double getPm25_24h() {
        return pm25_24h;
    }

    public void setPm25_24h(Double pm25_24h) {
        this.pm25_24h = pm25_24h;
    }

    public Double getPm10_24h() {
        return pm10_24h;
    }

    public void setPm10_24h(Double pm10_24h) {
        this.pm10_24h = pm10_24h;
    }

    public Double getNo2_24h() {
        return no2_24h;
    }

    public void setNo2_24h(Double no2_24h) {
        this.no2_24h = no2_24h;
    }

    public Double getSo2_24h() {
        return so2_24h;
    }

    public void setSo2_24h(Double so2_24h) {
        this.so2_24h = so2_24h;
    }

    public Double getO3_8h() {
        return o3_8h;
    }

    public void setO3_8h(Double o3_8h) {
        this.o3_8h = o3_8h;
    }

    public Double getCo_8h_mgm3() {
        return co_8h_mgm3;
    }

    public void setCo_8h_mgm3(Double co_8h_mgm3) {
        this.co_8h_mgm3 = co_8h_mgm3;
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

