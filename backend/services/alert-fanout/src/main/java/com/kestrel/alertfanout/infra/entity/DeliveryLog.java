package com.kestrel.alertfanout.infra.entity;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.Provenance;
import com.kestrel.common.events.Severity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.UUID;

@DynamoDbBean
public class DeliveryLog {

    private Long id;
    private String deliveryId;
    private String alertId;
    private String locationId;
    private String locationName;
    private Severity severity;
    private Channel channel;
    private String recipient;
    private Language language;
    private Instant timestamp;
    private String messageExcerpt;
    private Provenance provenance;
    private String status;
    private Long ttl;

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
        this.deliveryId = UUID.randomUUID().toString();
        this.alertId = alertId;
        this.locationId = locationId;
        this.locationName = locationName;
        this.severity = severity;
        this.channel = channel;
        this.recipient = recipient;
        this.language = language;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.messageExcerpt = messageExcerpt;
        this.provenance = provenance;
        this.status = status;
        this.ttl = this.timestamp.getEpochSecond() + (30L * 24 * 3600);
        this.id = Math.abs((long) this.deliveryId.hashCode());
    }

    public Long getId() {
        if (id == null && deliveryId != null) {
            id = Math.abs((long) deliveryId.hashCode());
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @DynamoDbPartitionKey
    public String getDeliveryId() {
        if (deliveryId == null) {
            deliveryId = UUID.randomUUID().toString();
        }
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "alertId-index")
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

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
