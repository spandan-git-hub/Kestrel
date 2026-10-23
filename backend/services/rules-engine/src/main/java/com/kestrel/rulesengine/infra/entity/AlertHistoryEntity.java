package com.kestrel.rulesengine.infra.entity;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "alert_history")
public class AlertHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", length = 64)
    private String alertId;

    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Hazard hazard;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_severity", nullable = false, length = 32)
    private Severity fromSeverity;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_severity", nullable = false, length = 32)
    private Severity toSeverity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Provenance provenance;

    @Column(name = "transitioned_at", nullable = false)
    private Instant transitionedAt;

    @Column(name = "first_exceedance_date")
    private LocalDate firstExceedanceDate;

    @Column(name = "peak_exceedance_share")
    private double peakExceedanceShare;

    @Column(length = 256)
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
    }

    public Long getId() {
        return id;
    }

    public String getAlertId() {
        return alertId;
    }

    public String getLocationId() {
        return locationId;
    }

    public Hazard getHazard() {
        return hazard;
    }

    public Severity getFromSeverity() {
        return fromSeverity;
    }

    public Severity getToSeverity() {
        return toSeverity;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public Instant getTransitionedAt() {
        return transitionedAt;
    }

    public LocalDate getFirstExceedanceDate() {
        return firstExceedanceDate;
    }

    public double getPeakExceedanceShare() {
        return peakExceedanceShare;
    }

    public String getReason() {
        return reason;
    }
}
