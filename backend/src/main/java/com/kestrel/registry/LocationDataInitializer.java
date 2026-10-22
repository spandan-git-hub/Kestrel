package com.kestrel.registry;

import com.kestrel.domain.AlertStateEntity;
import com.kestrel.domain.Hazard;
import com.kestrel.domain.Language;
import com.kestrel.domain.LocationEntity;
import com.kestrel.domain.Provenance;
import com.kestrel.domain.Severity;
import com.kestrel.repository.AlertStateRepository;
import com.kestrel.repository.LocationRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LocationDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocationDataInitializer.class);

    private final LocationRepository locationRepository;
    private final AlertStateRepository alertStateRepository;

    public LocationDataInitializer(LocationRepository locationRepository, AlertStateRepository alertStateRepository) {
        this.locationRepository = locationRepository;
        this.alertStateRepository = alertStateRepository;
    }

    @Override
    public void run(String... args) {
        if (locationRepository.count() == 0) {
            log.info("Seeding 4 Indian pilot locations into H2 database...");

            List<LocationEntity> pilotLocations = List.of(
                LocationEntity.builder()
                    .id("mumbai-kurla")
                    .name("Kurla Ward L, Mumbai")
                    .state("Maharashtra")
                    .latitude(19.0726)
                    .longitude(72.8797)
                    .riverName("Mithi River")
                    .threshold2yr(45.0)
                    .threshold5yr(85.0)
                    .threshold20yr(140.0)
                    .primaryLanguage(Language.mr)
                    .build(),
                LocationEntity.builder()
                    .id("patna-ward12")
                    .name("Ward 12, Patna")
                    .state("Bihar")
                    .latitude(25.6127)
                    .longitude(85.1589)
                    .riverName("Ganges River")
                    .threshold2yr(38000.0)
                    .threshold5yr(48000.0)
                    .threshold20yr(62000.0)
                    .primaryLanguage(Language.hi)
                    .build(),
                LocationEntity.builder()
                    .id("guwahati-river")
                    .name("Riverside, Guwahati")
                    .state("Assam")
                    .latitude(26.1856)
                    .longitude(91.7478)
                    .riverName("Brahmaputra River")
                    .threshold2yr(32000.0)
                    .threshold5yr(42000.0)
                    .threshold20yr(55000.0)
                    .primaryLanguage(Language.en)
                    .build(),
                LocationEntity.builder()
                    .id("cuttack-delta")
                    .name("Cuttack Municipal Corp")
                    .state("Odisha")
                    .latitude(20.4625)
                    .longitude(85.8830)
                    .riverName("Mahanadi River")
                    .threshold2yr(18000.0)
                    .threshold5yr(26000.0)
                    .threshold20yr(35000.0)
                    .primaryLanguage(Language.en)
                    .build()
            );

            locationRepository.saveAll(pilotLocations);
            log.info("Successfully seeded {} pilot locations", pilotLocations.size());

            // Initialize baseline AlertState for each location
            for (LocationEntity loc : pilotLocations) {
                if (!alertStateRepository.existsById(loc.getId())) {
                    AlertStateEntity state = AlertStateEntity.builder()
                        .locationId(loc.getId())
                        .hazard(Hazard.FLOOD)
                        .currentSeverity(Severity.NONE)
                        .candidateSeverity(Severity.NONE)
                        .candidateRuns(0)
                        .activeAlertId(null)
                        .provenance(Provenance.MODEL_ONLY)
                        .lastSignalAt(Instant.now())
                        .lastTransitionAt(Instant.now())
                        .firstExceedanceDate(null)
                        .peakShare(0.0)
                        .isStale(false)
                        .build();
                    alertStateRepository.save(state);
                }
            }
            log.info("Successfully initialized baseline AlertState for all pilot locations");
        }
    }
}
