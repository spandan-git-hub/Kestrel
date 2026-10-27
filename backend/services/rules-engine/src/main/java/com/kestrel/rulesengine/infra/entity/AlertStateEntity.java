package com.kestrel.rulesengine.infra.entity;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.time.LocalDate;

@DynamoDbBean
public class AlertStateEntity {

    private String locationId;
    private String locationName;
    private Hazard hazard;
    private Severity currentSeverity;
    private Severity candidateSeverity;
    private int candidateRuns;
    private String activeAlertId;
    private Provenance provenance;
    private LocalDate firstExceedanceDate;
    private double peakExceedanceShare;
    private Instant dataAsOf;
    private Instant lastTransitionAt;
    private Instant lastNotificationAt;
    private boolean isStale;
    private Long ttl;

    public AlertStateEntity() {}

    public AlertStateEntity(
            String locationId,
            String locationName,
            Hazard hazard,
            Severity currentSeverity,
            Severity candidateSeverity,
            int candidateRuns,
            String activeAlertId,
            Provenance provenance,
            LocalDate firstExceedanceDate,
            double peakExceedanceShare,
            Instant dataAsOf,
            Instant lastTransitionAt,
            Instant lastNotificationAt,
            boolean isStale) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.hazard = hazard;
        this.currentSeverity = currentSeverity;
        this.candidateSeverity = candidateSeverity;
        this.candidateRuns = candidateRuns;
        this.activeAlertId = activeAlertId;
        this.provenance = provenance;
        this.firstExceedanceDate = firstExceedanceDate;
        this.peakExceedanceShare = peakExceedanceShare;
        this.dataAsOf = dataAsOf;
        this.lastTransitionAt = lastTransitionAt;
        this.lastNotificationAt = lastNotificationAt;
        this.isStale = isStale;
        this.ttl = Instant.now().getEpochSecond() + (90L * 24 * 3600);
    }

    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Hazard getHazard() {
        return hazard;
    }

    public void setHazard(Hazard hazard) {
        this.hazard = hazard;
    }

    public Severity getCurrentSeverity() {
        return currentSeverity;
    }

    public void setCurrentSeverity(Severity currentSeverity) {
        this.currentSeverity = currentSeverity;
    }

    public Severity getCandidateSeverity() {
        return candidateSeverity;
    }

    public void setCandidateSeverity(Severity candidateSeverity) {
        this.candidateSeverity = candidateSeverity;
    }

    public int getCandidateRuns() {
        return candidateRuns;
    }

    public void setCandidateRuns(int candidateRuns) {
        this.candidateRuns = candidateRuns;
    }

    public String getActiveAlertId() {
        return activeAlertId;
    }

    public void setActiveAlertId(String activeAlertId) {
        this.activeAlertId = activeAlertId;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
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

    public Instant getDataAsOf() {
        return dataAsOf;
    }

    public void setDataAsOf(Instant dataAsOf) {
        this.dataAsOf = dataAsOf;
    }

    public Instant getLastTransitionAt() {
        return lastTransitionAt;
    }

    public void setLastTransitionAt(Instant lastTransitionAt) {
        this.lastTransitionAt = lastTransitionAt;
    }

    public Instant getLastNotificationAt() {
        return lastNotificationAt;
    }

    public void setLastNotificationAt(Instant lastNotificationAt) {
        this.lastNotificationAt = lastNotificationAt;
    }

    public boolean isStale() {
        return isStale;
    }

    public void setStale(boolean stale) {
        isStale = stale;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
