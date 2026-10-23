package com.kestrel.subscriberregistry.app;

import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.SubscriptionChangedPayload;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionRequest;
import com.kestrel.subscriberregistry.api.dto.CreateSubscriptionResponse;
import com.kestrel.subscriberregistry.api.dto.LocationResponse;
import com.kestrel.subscriberregistry.api.dto.SubscriptionResponse;
import com.kestrel.subscriberregistry.infra.entity.LocationEntity;
import com.kestrel.subscriberregistry.infra.entity.SubscriptionEntity;
import com.kestrel.subscriberregistry.infra.publisher.LocationEventPublisher;
import com.kestrel.subscriberregistry.infra.publisher.SubscriptionEventPublisher;
import com.kestrel.subscriberregistry.infra.repository.LocationRepository;
import com.kestrel.subscriberregistry.infra.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    private final LocationRepository locationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LocationEventPublisher locationEventPublisher;
    private final SubscriptionEventPublisher subscriptionEventPublisher;

    public RegistryService(
            LocationRepository locationRepository,
            SubscriptionRepository subscriptionRepository,
            LocationEventPublisher locationEventPublisher,
            SubscriptionEventPublisher subscriptionEventPublisher) {
        this.locationRepository = locationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.locationEventPublisher = locationEventPublisher;
        this.subscriptionEventPublisher = subscriptionEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(LocationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocationById(String id) {
        LocationEntity entity = locationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Monitored location not found: " + id));
        return LocationResponse.fromEntity(entity);
    }

    public CreateSubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        // Validate location exists
        if (!locationRepository.existsById(request.locationId())) {
            throw new NoSuchElementException("Cannot subscribe: location not found: " + request.locationId());
        }

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
        log.info("Registered subscription token={} for location={}, channel={}",
                token, request.locationId(), request.channel());

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

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(String token) {
        SubscriptionEntity entity = subscriptionRepository.findById(token)
                .orElseThrow(() -> new NoSuchElementException("Subscription not found for token: " + token));
        return SubscriptionResponse.fromEntity(entity);
    }

    @Transactional(readOnly = true)
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

        subscriptionRepository.delete(entity);
        log.info("Erased subscription token={} for locationId={} (GDPR/DPDP compliance)",
                token, entity.getLocationId());

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
                    loc.getId(),
                    loc.getName(),
                    loc.getState(),
                    loc.getLatitude(),
                    loc.getLongitude(),
                    loc.getRiverName(),
                    loc.getThreshold2yr(),
                    loc.getThreshold5yr(),
                    loc.getThreshold20yr(),
                    loc.getPrimaryLanguage(),
                    loc.isActive()
            );
            locationEventPublisher.publish(payload);
        }
        log.info("Synced {} active locations to downstream services", activeLocations.size());
        return activeLocations.size();
    }
}
