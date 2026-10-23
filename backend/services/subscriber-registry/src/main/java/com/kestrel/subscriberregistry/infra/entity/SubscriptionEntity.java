package com.kestrel.subscriberregistry.infra.entity;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
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
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String token;

    @Column(nullable = false, length = 64)
    private String locationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Channel channel;

    @Column(nullable = false, length = 128)
    private String recipientIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Severity minSeverity;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public SubscriptionEntity() {
    }

    public SubscriptionEntity(
            String token,
            String locationId,
            Channel channel,
            String recipientIdentifier,
            Language language,
            Severity minSeverity,
            String status) {
        this.token = token != null ? token : UUID.randomUUID().toString();
        this.locationId = locationId;
        this.channel = channel;
        this.recipientIdentifier = recipientIdentifier;
        this.language = language;
        this.minSeverity = minSeverity;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Severity getMinSeverity() {
        return minSeverity;
    }

    public void setMinSeverity(Severity minSeverity) {
        this.minSeverity = minSeverity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
