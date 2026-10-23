package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
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

@Entity
@Table(name = "delivery_logs")
public class DeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, length = 64)
    private String alertId;

    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(name = "location_name", nullable = false, length = 128)
    private String locationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Channel channel;

    @Column(nullable = false, length = 128)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Language language;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "message_excerpt", nullable = false, length = 1024)
    private String messageExcerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Provenance provenance;

    @Column(nullable = false, length = 32)
    private String status;

    public DeliveryLog() {}

    public DeliveryLog(
            String alertId,
            String locationId,
            String locationName,
            Severity severity,
            Channel channel,
            String recipient,
            Language language,
            Instant timestamp,
            String messageExcerpt,
            Provenance provenance,
            String status) {
        this.alertId = alertId;
        this.locationId = locationId;
        this.locationName = locationName;
        this.severity = severity;
        this.channel = channel;
        this.recipient = recipient;
        this.language = language;
        this.timestamp = timestamp;
        this.messageExcerpt = messageExcerpt;
        this.provenance = provenance;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
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

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageExcerpt() {
        return messageExcerpt;
    }

    public void setMessageExcerpt(String messageExcerpt) {
        this.messageExcerpt = messageExcerpt;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
