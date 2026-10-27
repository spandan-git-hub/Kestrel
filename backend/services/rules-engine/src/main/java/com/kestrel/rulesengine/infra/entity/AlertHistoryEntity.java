package com.kestrel.rulesengine.infra.entity;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.time.LocalDate;

@DynamoDbBean
public class AlertHistoryEntity {

    private Long id;
    private String alertId;
    private String locationId;
    private Hazard hazard;
    private Severity fromSeverity;
    private Severity toSeverity;
    private Provenance provenance;
    private Instant transitionedAt;
    private LocalDate firstExceedanceDate;
    private double peakExceedanceShare;
    private String reason;

    public AlertHistoryEntity() {}

    public AlertHistoryEntity(
            String alertId,
            String locationId,
            Hazard hazard,
            Severity fromSeverity,
            Severity toSeverity,
            Provenance provenance,
            Instant transitionedAt,
            LocalDate firstExceedanceDate,
            double peakExceedanceShare,
            String reason) {
        this.alertId = alertId;
        this.locationId = locationId;
        this.hazard = hazard;
        this.fromSeverity = fromSeverity;
        this.toSeverity = toSeverity;
        this.provenance = provenance;
        this.transitionedAt = transitionedAt;
        this.firstExceedanceDate = firstExceedanceDate;
        this.peakExceedanceShare = peakExceedanceShare;
        this.reason = reason;
        this.id = Math.abs((long) (locationId + "#" + (alertId != null ? alertId : "") + "#" + (transitionedAt != null ? transitionedAt.toString() : "")).hashCode());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Hazard getHazard() {
        return hazard;
    }

    public void setHazard(Hazard hazard) {
        this.hazard = hazard;
    }

    public Severity getFromSeverity() {
        return fromSeverity;
    }

    public void setFromSeverity(Severity fromSeverity) {
        this.fromSeverity = fromSeverity;
    }

    public Severity getToSeverity() {
        return toSeverity;
    }

    public void setToSeverity(Severity toSeverity) {
        this.toSeverity = toSeverity;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    public Instant getTransitionedAt() {
        return transitionedAt;
    }

    public void setTransitionedAt(Instant transitionedAt) {
        this.transitionedAt = transitionedAt;
    }

    public LocalDate getFirstExceedanceDate() {
        return firstExceedanceDate;
    }

    public void setFirstExceedanceDate(LocalDate firstExceedanceDate) {
        this.firstExceedanceDate = firstExceedanceDate;
    }

    public double getPeakExceedanceShare() {
        return peakExceedanceShare;
    }

    public void setPeakExceedanceShare(double peakExceedanceShare) {
        this.peakExceedanceShare = peakExceedanceShare;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
