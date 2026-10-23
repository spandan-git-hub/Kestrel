package com.kestrel.rulesengine.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "forecast_snapshots_replica")
public class ForecastSnapshotReplicaEntity {

    @Id
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(name = "data_as_of", nullable = false)
    private Instant dataAsOf;

    @Column(name = "is_simulated", nullable = false)
    private boolean isSimulated;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Lob
    @Column(name = "forecast_days_json", nullable = false)
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
