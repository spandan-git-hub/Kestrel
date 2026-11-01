package com.kestrel.alertfanout.app;

import com.kestrel.alertfanout.infra.entity.LocationReplica;
import com.kestrel.alertfanout.infra.repository.LocationReplicaRepository;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.LocationUpsertedPayload;
import com.kestrel.common.events.Provenance;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class LocationReplicaService {

    private static final Logger log = LoggerFactory.getLogger(LocationReplicaService.class);

    private final LocationReplicaRepository replicaRepository;

    public LocationReplicaService(LocationReplicaRepository replicaRepository) {
        this.replicaRepository = replicaRepository;
    }

    @PostConstruct
    public void seedPilotLocationsIfEmpty() {
        if (replicaRepository.count() == 0) {
            log.info("Location replica table empty in alert-fanout. Seeding 4 Indian pilot locations...");
            List<LocationReplica> seeds = List.of(
                    new LocationReplica("mumbai-kurla", "Kurla Ward L, Mumbai", "Maharashtra",
                            19.0726, 72.8797, "Mithi River", 45.0, 85.0, 140.0, Language.mr, true),
                    new LocationReplica("patna-ward12", "Ward 12, Patna", "Bihar",
                            25.6127, 85.1589, "Ganges River", 38000.0, 48000.0, 62000.0, Language.hi, true),
                    new LocationReplica("guwahati-river", "Riverside, Guwahati", "Assam",
                            26.1856, 91.7478, "Brahmaputra River", 32000.0, 42000.0, 55000.0, Language.en, true),
                    new LocationReplica("cuttack-delta", "Cuttack Municipal Corp", "Odisha",
                            20.4625, 85.8830, "Mahanadi River", 18000.0, 26000.0, 35000.0, Language.en, true)
            );
            replicaRepository.saveAll(seeds);
            log.info("Seeded 4 pilot location replicas in alert-fanout");
        }
    }

    public LocationReplica upsertLocationReplica(LocationUpsertedPayload payload) {
        LocationReplica replica = replicaRepository.findById(payload.locationId())
                .orElse(new LocationReplica());

        replica.setLocationId(payload.locationId());
        replica.setName(payload.name());
        replica.setState(payload.state());
        replica.setRegionType(payload.regionType());
        replica.setLatitude(payload.latitude());
        replica.setLongitude(payload.longitude());
        replica.setRiverName(payload.riverName());
        replica.setThreshold2yr(payload.threshold2yr());
        replica.setThreshold5yr(payload.threshold5yr());
        replica.setThreshold20yr(payload.threshold20yr());
        replica.setPrimaryLanguage(payload.primaryLanguage());
        replica.setActive(payload.active());
        if (replica.getCurrentProvenance() == null) {
            replica.setCurrentProvenance(Provenance.MODEL_ONLY);
        }

        LocationReplica saved = replicaRepository.save(replica);
        log.info("Upserted LocationReplica: locationId={}, name={}, state={}",
                saved.getLocationId(), saved.getName(), saved.getState());
        return saved;
    }

    public Optional<LocationReplica> getLocation(String locationId) {
        return replicaRepository.findById(locationId);
    }

    public List<LocationReplica> getAllLocations() {
        return replicaRepository.findAll();
    }

    public Optional<LocationReplica> updateProvenance(String locationId, Provenance newProvenance) {
        return replicaRepository.findById(locationId).map(loc -> {
            loc.setCurrentProvenance(newProvenance);
            return replicaRepository.save(loc);
        });
    }

    public Optional<LocationReplica> recordGroundReport(String locationId) {
        return replicaRepository.findById(locationId).map(loc -> {
            loc.setReportCount(loc.getReportCount() + 1);
            loc.setLastReportedAt(Instant.now());
            loc.setCurrentProvenance(Provenance.MODEL_PLUS_REPORTS);
            log.info("Location {} provenance upgraded to MODEL_PLUS_REPORTS after ground report (total reports: {})",
                    locationId, loc.getReportCount());
            return replicaRepository.save(loc);
        });
    }
}
