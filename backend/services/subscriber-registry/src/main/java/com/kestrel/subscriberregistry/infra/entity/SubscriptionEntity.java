package com.kestrel.subscriberregistry.infra.entity;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@DynamoDbBean
public class SubscriptionEntity {

    private String token;
    private String locationId;
    private Channel channel;
    private String recipientIdentifier;
    private Language language;
    private Severity minSeverity;
    private Set<String> hazards;
    private String status = "ACTIVE";
    private Instant lastNotifiedAt;
    private Severity lastNotifiedSeverity;
    private Instant createdAt;
    private Instant updatedAt;
    private Long ttl;

    public SubscriptionEntity() {
    }

    public SubscriptionEntity(
            String token,
            String locationId,
            Channel channel,
            String recipientIdentifier,
            Language language,
            Severity minSeverity,
            Set<Hazard> hazards,
            String status) {
        this.token = token != null && !token.isBlank() ? token : UUID.randomUUID().toString();
        this.locationId = locationId;
        this.channel = channel;
        this.recipientIdentifier = recipientIdentifier;
        this.language = language;
        this.minSeverity = minSeverity;
        this.hazards = (hazards != null && !hazards.isEmpty())
                ? hazards.stream().map(Hazard::name).collect(Collectors.toSet())
                : Set.of(Hazard.FLOOD.name(), Hazard.HEAT.name(), Hazard.AIR_QUALITY.name());
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public SubscriptionEntity(
            String token,
            String locationId,
            Channel channel,
            String recipientIdentifier,
            Language language,
            Severity minSeverity,
            String status) {
        this(token, locationId, channel, recipientIdentifier, language, minSeverity, Set.of(Hazard.FLOOD, Hazard.HEAT, Hazard.AIR_QUALITY), status);
    }

    @DynamoDbPartitionKey
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "locationId-index")
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
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

    @DynamoDbAttribute("hazards")
    public Set<String> getHazards() {
        if (hazards == null || hazards.isEmpty()) {
            return Set.of(Hazard.FLOOD.name(), Hazard.HEAT.name(), Hazard.AIR_QUALITY.name());
        }
        return hazards;
    }

    public void setHazards(Set<String> hazards) {
        this.hazards = hazards;
    }

    @DynamoDbIgnore
    public Set<Hazard> getHazardsAsEnums() {
        if (hazards == null || hazards.isEmpty()) {
            return Set.of(Hazard.FLOOD, Hazard.HEAT, Hazard.AIR_QUALITY);
        }
        Set<Hazard> result = new HashSet<>();
        for (String h : hazards) {
            try {
                result.add(Hazard.valueOf(h.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return result.isEmpty() ? Set.of(Hazard.FLOOD, Hazard.HEAT, Hazard.AIR_QUALITY) : result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(Instant lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public Severity getLastNotifiedSeverity() {
        return lastNotifiedSeverity;
    }

    public void setLastNotifiedSeverity(Severity lastNotifiedSeverity) {
        this.lastNotifiedSeverity = lastNotifiedSeverity;
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
