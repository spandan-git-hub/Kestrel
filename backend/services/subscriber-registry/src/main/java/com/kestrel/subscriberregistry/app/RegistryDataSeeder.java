package com.kestrel.subscriberregistry.app;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.subscriberregistry.infra.entity.LocationEntity;
import com.kestrel.subscriberregistry.infra.publisher.LocationEventPublisher;
import com.kestrel.subscriberregistry.infra.repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegistryDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegistryDataSeeder.class);

    public static final List<LocationEntity> SEED_LOCATIONS = IndianRiverCatalog.ALL_45_STATIONS.stream()
            .map(station -> new LocationEntity(
                    station.locationId(),
                    station.name(),
                    station.state(),
                    station.riverName(),
                    station.basin(),
                    station.latitude(),
                    station.longitude(),
                    station.threshold2yr(),
                    station.threshold5yr(),
                    station.threshold20yr(),
                    station.primaryLanguage(),
                    station.active()
            ))
            .toList();

    private final LocationRepository locationRepository;
    private final LocationEventPublisher locationEventPublisher;

    public RegistryDataSeeder(LocationRepository locationRepository, LocationEventPublisher locationEventPublisher) {
        this.locationRepository = locationRepository;
        this.locationEventPublisher = locationEventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        long currentCount = locationRepository.count();
        if (currentCount < SEED_LOCATIONS.size()) {
            log.info("Bootstrapping All-India 45 River Catalog into subscriber-registry (current count: {}, target: {})",
                    currentCount, SEED_LOCATIONS.size());

            int newCount = 0;
            for (LocationEntity seed : SEED_LOCATIONS) {
                if (!locationRepository.existsById(seed.getLocationId())) {
                    LocationEntity saved = locationRepository.save(seed);
                    newCount++;

                    LocationUpsertedPayload payload = new LocationUpsertedPayload(
                            saved.getLocationId(),
                            saved.getName(),
                            saved.getState(),
                            saved.getLatitude(),
                            saved.getLongitude(),
                            saved.getRiverName(),
                            saved.getBasin(),
                            saved.getThreshold2yr(),
                            saved.getThreshold5yr(),
                            saved.getThreshold20yr(),
                            saved.getPrimaryLanguage(),
                            saved.isActive()
                    );
                    locationEventPublisher.publish(payload);
                }
            }
            log.info("Finished bootstrapping {} Indian river stations into subscriber-registry (total now: {})",
                    newCount, locationRepository.count());
        } else {
            log.info("Database already contains {} river stations, skipping seed", currentCount);
        }
    }
}
