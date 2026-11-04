package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.infra.entity.SubscriberReplica;
import com.kestrel.alertfanout.infra.repository.SubscriberReplicaRepository;
import com.kestrel.common.events.Channel;
import com.kestrel.common.events.Hazard;
import com.kestrel.common.events.Severity;
import com.kestrel.common.events.SubscriptionChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubscriberReplicaService {

    private static final Logger log = LoggerFactory.getLogger(SubscriberReplicaService.class);

    private final SubscriberReplicaRepository replicaRepository;

    public SubscriberReplicaService(SubscriberReplicaRepository replicaRepository) {
        this.replicaRepository = replicaRepository;
    }

    public SubscriberReplica replicateSubscription(SubscriptionChangedPayload payload) {
        if (payload == null || payload.token() == null || payload.token().isBlank()) {
            log.warn("Cannot replicate subscription with null or blank token");
            return null;
        }

        if ("DELETED".equalsIgnoreCase(payload.action())) {
            log.info("Deleting replicated subscriber for token={}, locationId={}", payload.token(), payload.locationId());
            if (payload.locationId() != null && !payload.locationId().isBlank()) {
                replicaRepository.deleteByLocationIdAndToken(payload.locationId(), payload.token());
            } else {
                replicaRepository.deleteByToken(payload.token());
            }
            return null;
        }

        log.info("Upserting subscriber replica: token={}, locationId={}, channel={}, minSeverity={}, hazards={}",
                payload.token(), payload.locationId(), payload.channel(), payload.minSeverity(), payload.hazards());

        SubscriberReplica replica = (payload.locationId() != null)
                ? replicaRepository.findByLocationIdAndToken(payload.locationId(), payload.token()).orElse(new SubscriberReplica())
                : new SubscriberReplica();

        replica.setLocationId(payload.locationId());
        replica.setToken(payload.token());
        replica.setChannel(payload.channel());
        replica.setRecipientIdentifier(payload.recipientIdentifier());
        replica.setLanguage(payload.language());
        replica.setMinSeverity(payload.minSeverity());
        if (payload.hazards() != null && !payload.hazards().isEmpty()) {
            replica.setHazards(payload.hazards().stream().map(Hazard::name).collect(Collectors.toSet()));
        } else {
            replica.setHazards(Set.of(Hazard.FLOOD.name(), Hazard.HEAT.name(), Hazard.AIR_QUALITY.name()));
        }
        replica.setStatus("ACTIVE");
        replica.setUpdatedAt(Instant.now());
        if (replica.getCreatedAt() == null) {
            replica.setCreatedAt(Instant.now());
        }

        return replicaRepository.save(replica);
    }

    /**
     * Filters active subscribers for a given location whose minSeverity threshold is met or exceeded
     * by the active alert severity and who are subscribed to the alert's hazard.
     */
    public List<SubscriberReplica> getActiveSubscribersForAlert(String locationId, Severity alertSeverity, Hazard hazard) {
        if (locationId == null || locationId.isBlank() || alertSeverity == null || alertSeverity == Severity.NONE) {
            return Collections.emptyList();
        }

        List<SubscriberReplica> locationSubscribers = replicaRepository.findByLocationId(locationId);

        return locationSubscribers.stream()
                .filter(sub -> sub.getStatus() != null && "ACTIVE".equalsIgnoreCase(sub.getStatus()))
                .filter(sub -> {
                    Severity minSev = sub.getMinSeverity();
                    if (minSev == null) return true;
                    return alertSeverity.ordinal() >= minSev.ordinal();
                })
                .filter(sub -> {
                    if (hazard == null) return true;
                    Set<String> subHazards = sub.getHazards();
                    return subHazards == null || subHazards.isEmpty() || subHazards.contains(hazard.name());
                })
                .toList();
    }

    public List<SubscriberReplica> getActiveSubscribersForAlert(String locationId, Severity alertSeverity) {
        return getActiveSubscribersForAlert(locationId, alertSeverity, null);
    }

    public List<SubscriberReplica> getSubscribersByLocation(String locationId) {
        if (locationId == null || locationId.isBlank()) {
            return Collections.emptyList();
        }
        return replicaRepository.findByLocationId(locationId);
    }

    public List<SubscriberReplica> getSubscribersByChannel(Channel channel) {
        if (channel == null) {
            return Collections.emptyList();
        }
        return replicaRepository.findByChannel(channel);
    }

    public Optional<SubscriberReplica> getSubscriber(String locationId, String token) {
        return replicaRepository.findByLocationIdAndToken(locationId, token);
    }

    public void updateSubscriberStatus(String locationId, String token, String status) {
        replicaRepository.findByLocationIdAndToken(locationId, token).ifPresent(sub -> {
            sub.setStatus(status);
            sub.setUpdatedAt(Instant.now());
            replicaRepository.save(sub);
            log.info("Updated subscriber status to {} for token={}, locationId={}", status, token, locationId);
        });
    }

    public void deleteSubscriber(String locationId, String token) {
        replicaRepository.deleteByLocationIdAndToken(locationId, token);
    }

    public SubscriberReplica saveSubscriber(SubscriberReplica subscriber) {
        if (subscriber == null) return null;
        subscriber.setUpdatedAt(Instant.now());
        return replicaRepository.save(subscriber);
    }

    public void updateNotificationTimestamp(String locationId, String token, Instant notifiedAt, Severity severity) {
        replicaRepository.findByLocationIdAndToken(locationId, token).ifPresent(sub -> {
            sub.setLastNotifiedAt(notifiedAt);
            sub.setLastNotifiedSeverity(severity);
            sub.setUpdatedAt(Instant.now());
            replicaRepository.save(sub);
            log.info("Updated subscriber notification timestamp: token={}, locationId={}, lastNotifiedAt={}, severity={}",
                    token, locationId, notifiedAt, severity);
        });
    }
}
