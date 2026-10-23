package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "active_alerts")
public class ActiveAlertEntity {

    @Id
    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(name = "alert_id", nullable = false, length = 64)
    private String alertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Hazard hazard;

    @Column(nullable = false, length = 32)
    private String stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_severity", nullable = false, length = 16)
    private Severity fromSeverity;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_severity", nullable = false, length = 16)
    private Severity toSeverity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Provenance provenance;

    @Column(name = "first_exceedance_date")
    private LocalDate firstExceedanceDate;

    @Column(name = "peak_share", nullable = false)
    private double peakShare;

    @Column(name = "auto_send", nullable = false)
    private boolean autoSend;

    @Column(name = "composed_english", length = 2048)
    private String composedEnglish;

    @Column(name = "composed_hindi", length = 2048)
    private String composedHindi;

    @Column(name = "composed_marathi", length = 2048)
    private String composedMarathi;

    @Column(name = "whatsapp_url", length = 4096)
    private String whatsAppUrl;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ActiveAlertEntity() {}

    public ActiveAlertEntity(
            String locationId,
            String alertId,
            Hazard hazard,
            String stage,
            Severity fromSeverity,
            Severity toSeverity,
            Provenance provenance,
            LocalDate firstExceedanceDate,
            double peakShare,
            boolean autoSend,
            Instant receivedAt) {
        this.locationId = locationId;
        this.alertId = alertId;
        this.hazard = hazard;
        this.stage = stage;
        this.fromSeverity = fromSeverity;
        this.toSeverity = toSeverity;
        this.provenance = provenance;
        this.firstExceedanceDate = firstExceedanceDate;
        this.peakShare = peakShare;
        this.autoSend = autoSend;
        this.receivedAt = receivedAt;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public Hazard getHazard() {
        return hazard;
    }

    public void setHazard(Hazard hazard) {
        this.hazard = hazard;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
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

    public LocalDate getFirstExceedanceDate() {
        return firstExceedanceDate;
    }

    public void setFirstExceedanceDate(LocalDate firstExceedanceDate) {
        this.firstExceedanceDate = firstExceedanceDate;
    }

    public double getPeakShare() {
        return peakShare;
    }

    public void setPeakShare(double peakShare) {
        this.peakShare = peakShare;
    }

    public boolean isAutoSend() {
        return autoSend;
    }

    public void setAutoSend(boolean autoSend) {
        this.autoSend = autoSend;
    }

    public String getComposedEnglish() {
        return composedEnglish;
    }

    public void setComposedEnglish(String composedEnglish) {
        this.composedEnglish = composedEnglish;
    }

    public String getComposedHindi() {
        return composedHindi;
    }

    public void setComposedHindi(String composedHindi) {
        this.composedHindi = composedHindi;
    }

    public String getComposedMarathi() {
        return composedMarathi;
    }

    public void setComposedMarathi(String composedMarathi) {
        this.composedMarathi = composedMarathi;
    }

    public String getWhatsAppUrl() {
        return whatsAppUrl;
    }

    public void setWhatsAppUrl(String whatsAppUrl) {
        this.whatsAppUrl = whatsAppUrl;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
