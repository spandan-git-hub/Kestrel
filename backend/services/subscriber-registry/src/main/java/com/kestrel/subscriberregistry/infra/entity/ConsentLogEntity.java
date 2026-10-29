package com.kestrel.subscriberregistry.infra.entity;

import com.kestrel.common.events.Channel;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log recording affirmative consent under DPDP Act 2023.
 */
@DynamoDbBean
public class ConsentLogEntity {

    private String consentId;
    private String token;
    private String locationId;
    private Channel channel;
    private String consentVersion;
    private String status;
    private Instant consentedAt;
    private Instant withdrawnAt;
    private Long ttl;

    public ConsentLogEntity() {
    }

    public ConsentLogEntity(String token, String locationId, Channel channel, String consentVersion) {
        this.consentId = UUID.randomUUID().toString();
        this.token = token;
        this.locationId = locationId;
        this.channel = channel;
        this.consentVersion = consentVersion != null && !consentVersion.isBlank() ? consentVersion : "DPDP-2023-v1.0";
        this.status = "GRANTED";
        this.consentedAt = Instant.now();
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("consentId")
    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "token-index")
    @DynamoDbAttribute("token")
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

    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getConsentedAt() {
        return consentedAt;
    }

    public void setConsentedAt(Instant consentedAt) {
        this.consentedAt = consentedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}

