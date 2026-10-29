package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class SubscriberReplica {

    private String locationId;
    private String token;
    private Channel channel;
    private String recipientIdentifier;
    private Language language = Language.en;
    private Severity minSeverity = Severity.WATCH;
    private String status = "ACTIVE";
    private Instant createdAt;
    private Instant updatedAt;
    private Long ttl;

    public SubscriberReplica() {
    }

    public SubscriberReplica(
            String locationId,
            String token,
            Channel channel,
            String recipientIdentifier,
            Language language,
            Severity minSeverity,
            String status) {
        this.locationId = locationId;
        this.token = token;
        this.channel = channel;
        this.recipientIdentifier = recipientIdentifier;
        this.language = language != null ? language : Language.en;
        this.minSeverity = minSeverity != null ? minSeverity : Severity.WATCH;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @DynamoDbSortKey
    @DynamoDbSecondarySortKey(indexNames = "channel-index")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "channel-index")
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

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}

