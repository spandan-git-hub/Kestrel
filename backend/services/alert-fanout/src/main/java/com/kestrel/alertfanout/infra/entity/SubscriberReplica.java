package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Severity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@DynamoDbBean
public class SubscriberReplica {

    private String locationId;
    private String token;
    private Channel channel;
    private String recipientIdentifier;
    private Language language = Language.en;
    private Severity minSeverity = Severity.WATCH;
    private Set<String> hazards;
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
            Set<Hazard> hazards,
            String status) {
        this.locationId = locationId;
        this.token = token;
        this.channel = channel;
        this.recipientIdentifier = recipientIdentifier;
        this.language = language != null ? language : Language.en;
        this.minSeverity = minSeverity != null ? minSeverity : Severity.WATCH;
        this.hazards = (hazards != null && !hazards.isEmpty())
                ? hazards.stream().map(Hazard::name).collect(Collectors.toSet())
                : Set.of(Hazard.FLOOD.name(), Hazard.HEAT.name(), Hazard.AIR_QUALITY.name());
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public SubscriberReplica(
            String locationId,
            String token,
            Channel channel,
            String recipientIdentifier,
            Language language,
            Severity minSeverity,
            String status) {
        this(locationId, token, channel, recipientIdentifier, language, minSeverity, Set.of(Hazard.FLOOD, Hazard.HEAT, Hazard.AIR_QUALITY), status);
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
