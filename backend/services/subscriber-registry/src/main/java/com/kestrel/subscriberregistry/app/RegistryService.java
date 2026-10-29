package com.kestrel.subscriberregistry.app;

import com.kestrel.common.events.Channel;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.SubscriptionChangedPayload;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionRequest;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionResponse;
import com.kestrel.subscriberregistry.api.dto.LocationResponse;
import com.kestrel.subscriberregistry.api.dto.SubscriptionResponse;
import com.kestrel.subscriberregistry.infra.entity.ConsentLogEntity;
import com.kestrel.subscriberregistry.infra.entity.LocationEntity;
import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;
import com.kestrel.subscriberregistry.infra.publisher.LocationEventPublisher;
import com.kestrel.subscriberregistry.infra.publisher.SubscriptionEventPublisher;
import com.kestrel.subscriberregistry.infra.repository.ConsentLogRepository;
import com.kestrel.subscriberregistry.infra.repository.LocationRepository;
import com.kestrel.subscriberregistry.infra.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    private final LocationRepository locationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ConsentLogRepository consentLogRepository;
    private final LocationEventPublisher locationEventPublisher;
    private final SubscriptionEventPublisher subscriptionEventPublisher;

    public RegistryService(
            LocationRepository locationRepository,
            SubscriptionRepository subscriptionRepository,
            ConsentLogRepository consentLogRepository,
            LocationEventPublisher locationEventPublisher,
            SubscriptionEventPublisher subscriptionEventPublisher) {
        this.locationRepository = locationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.consentLogRepository = consentLogRepository;
        this.locationEventPublisher = locationEventPublisher;
        this.subscriptionEventPublisher = subscriptionEventPublisher;
    }

    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(LocationResponse::fromEntity)
                .toList();
    }

    public LocationResponse getLocationById(String id) {
        LocationEntity entity = locationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Monitored location not found: " + id));
        return LocationResponse.fromEntity(entity);
    }

    public CreateSubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        // 1. Enforce DPDP Act 2023: opt-in consent requirement
        if (Boolean.FALSE.equals(request.consentGiven())) {
            throw new IllegalArgumentException("Explicit opt-in consent is required under DPDP Act 2023 to register for disaster alerts.");
        }

        // 2. Enforce DPDP Act 2023: Zero-PII rule (no phone numbers or email addresses)
        if (PiiValidator.containsPii(request.recipientIdentifier())) {
            throw new IllegalArgumentException("DPDP Act 2023 compliance violation: Personally Identifiable Information (phone numbers, email addresses) cannot be collected. Use anonymous Telegram Chat ID or Web Push tokens.");
        }

        // 3. Enforce channel delivery compliance
        if (request.channel() == Channel.WHATSAPP_LINK) {
            throw new IllegalArgumentException("DPDP Act 2023 compliance: Automated phone subscriptions are not permitted. WhatsApp delivery operates exclusively via coordinator community group forward links (wa.me). Please subscribe via Telegram or Web Push.");
        }

        // 4. Validate location exists
        if (!locationRepository.existsById(request.locationId())) {
            throw new NoSuchElementException("Cannot subscribe: location not found: " + request.locationId());
        }

        // 5. Generate anonymous UUID token
        String token = UUID.randomUUID().toString();
        SubscriptionEntity entity = new SubscriptionEntity(
                token,
                request.locationId(),
                request.channel(),
                request.recipientIdentifier(),
                request.language(),
                request.minSeverity(),
                "ACTIVE"
        );
        subscriptionRepository.save(entity);

        // 6. Record versioned opt-in consent in audit log (DPDP Act 2023)
        ConsentLogEntity consentLog = new ConsentLogEntity(
                token,
                request.locationId(),
                request.channel(),
                request.consentVersion()
        );
        consentLogRepository.save(consentLog);

        log.info("Registered subscription token={} for location={}, channel={} with DPDP consent audit consentId={}",
                token, request.locationId(), request.channel(), consentLog.getConsentId());

        // 7. Publish SubscriptionChanged event to SNS
        subscriptionEventPublisher.publish(new SubscriptionChangedPayload(
                token,
                request.locationId(),
                request.channel(),
                request.recipientIdentifier(),
                request.language(),
                request.minSeverity(),
                "CREATED"
        ));

        return new CreateSubscriptionResponse(token, "ACTIVE", "Subscription registered.");
    }

    public SubscriptionResponse getSubscription(String token) {
        SubscriptionEntity entity = subscriptionRepository.findById(token)
                .orElseThrow(() -> new NoSuchElementException("Subscription not found for token: " + token));
        return SubscriptionResponse.fromEntity(entity);
    }

    public List<SubscriptionResponse> getSubscriptions(String locationId) {
        List<SubscriptionEntity> entities;
        if (locationId != null && !locationId.isBlank()) {
            entities = subscriptionRepository.findByLocationId(locationId);
        } else {
            entities = subscriptionRepository.findAll();
        }
        return entities.stream()
                .map(SubscriptionResponse::fromEntity)
                .toList();
    }

    public void deleteSubscription(String token) {
        SubscriptionEntity entity = subscriptionRepository.findById(token)
                .orElseThrow(() -> new NoSuchElementException("Subscription not found for token: " + token));

        // 1. Hard delete subscription entity (DPDP Act 2023 right to erasure)
        subscriptionRepository.delete(entity);
        log.info("Erased subscription token={} for locationId={} (DPDP Act 2023 right to erasure)",
                token, entity.getLocationId());

        // 2. Mark consent withdrawn in audit log
        List<ConsentLogEntity> consentLogs = consentLogRepository.findByToken(token);
        for (ConsentLogEntity logEntry : consentLogs) {
            logEntry.setStatus("WITHDRAWN");
            logEntry.setWithdrawnAt(Instant.now());
            consentLogRepository.save(logEntry);
        }

        // 3. Publish SubscriptionChanged event with action=DELETED to SNS
        subscriptionEventPublisher.publish(new SubscriptionChangedPayload(
                token,
                entity.getLocationId(),
                entity.getChannel(),
                entity.getRecipientIdentifier(),
                entity.getLanguage(),
                entity.getMinSeverity(),
                "DELETED"
        ));
    }

    public int syncLocations() {
        List<LocationEntity> activeLocations = locationRepository.findByActiveTrue();
        for (LocationEntity loc : activeLocations) {
            LocationUpsertedPayload payload = new LocationUpsertedPayload(
                    loc.getLocationId(),
                    loc.getName(),
                    loc.getState(),
                    loc.getLatitude(),
                    loc.getLongitude(),
                    loc.getRiverName(),
                    loc.getBasin(),
                    loc.getThreshold2yr(),
                    loc.getThreshold5yr(),
                    loc.getThreshold20yr(),
                    loc.getPrimaryLanguage(),
                    loc.isActive()
            );
            locationEventPublisher.publish(payload);
        }
        log.info("Synced {} active Indian river locations to downstream services", activeLocations.size());
        return activeLocations.size();
    }
}
