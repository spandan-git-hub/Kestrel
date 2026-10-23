package com.kestrel.subscriberregistry.app;

import com.kestrel.common.events.Language;
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

    public static final List<LocationEntity> SEED_LOCATIONS = List.of(
            new LocationEntity("mumbai-kurla", "Kurla Ward L, Mumbai", "Maharashtra",
                    19.0726, 72.8797, "Mithi River", 45.0, 85.0, 140.0, Language.mr, true),
            new LocationEntity("patna-ward12", "Ward 12, Patna", "Bihar",
                    25.6127, 85.1589, "Ganges River", 38000.0, 48000.0, 62000.0, Language.hi, true),
            new LocationEntity("guwahati-river", "Riverside, Guwahati", "Assam",
                    26.1856, 91.7478, "Brahmaputra River", 32000.0, 42000.0, 55000.0, Language.en, true),
            new LocationEntity("cuttack-delta", "Cuttack Municipal Corp", "Odisha",
                    20.4625, 85.8830, "Mahanadi River", 18000.0, 26000.0, 35000.0, Language.en, true)
    );

    private final LocationRepository locationRepository;
    private final LocationEventPublisher locationEventPublisher;

    public RegistryDataSeeder(LocationRepository locationRepository, LocationEventPublisher locationEventPublisher) {
        this.locationRepository = locationRepository;
        this.locationEventPublisher = locationEventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (locationRepository.count() == 0) {
            log.info("Seeding {} Indian pilot locations in subscriber-registry", SEED_LOCATIONS.size());
            List<LocationEntity> saved = locationRepository.saveAll(SEED_LOCATIONS);

            for (LocationEntity loc : saved) {
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
            log.info("Finished seeding pilot locations and published LocationUpserted events");
        } else {
            log.info("Database already contains {} locations, skipping seed", locationRepository.count());
        }
    }
}
