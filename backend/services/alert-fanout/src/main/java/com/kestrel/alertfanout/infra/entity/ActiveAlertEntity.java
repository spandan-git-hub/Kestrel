package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.time.LocalDate;

@DynamoDbBean
public class ActiveAlertEntity {

    private String locationId;
    private String alertId;
    private Hazard hazard;
    private String stage;
    private Severity fromSeverity;
    private Severity toSeverity;
    private Provenance provenance;
    private LocalDate firstExceedanceDate;
    private double peakShare;
    private boolean autoSend;
    private String composedEnglish;
    private String composedHindi;
    private String composedMarathi;
    private String whatsAppUrl;
    private Instant receivedAt;
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

    @DynamoDbPartitionKey
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
