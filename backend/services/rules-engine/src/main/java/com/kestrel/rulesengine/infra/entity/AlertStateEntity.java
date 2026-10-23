package com.kestrel.rulesengine.infra.entity;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "alert_state")
public class AlertStateEntity {

    @Id
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(name = "location_name", nullable = false, length = 128)
    private String locationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Hazard hazard;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_severity", nullable = false, length = 32)
    private Severity currentSeverity;

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_severity", nullable = false, length = 32)
    private Severity candidateSeverity;

    @Column(name = "candidate_runs", nullable = false)
    private int candidateRuns;

    @Column(name = "active_alert_id", length = 64)
    private String activeAlertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Provenance provenance;

    @Column(name = "first_exceedance_date")
    private LocalDate firstExceedanceDate;

    @Column(name = "peak_exceedance_share")
    private double peakExceedanceShare;

    @Column(name = "data_as_of")
    private Instant dataAsOf;

    @Column(name = "last_transition_at")
    private Instant lastTransitionAt;

    @Column(name = "last_notification_at")
    private Instant lastNotificationAt;

    @Column(name = "is_stale", nullable = false)
    private boolean isStale;

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
    }

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
}
