package com.kestrel.aqingest.app;

import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.repository.AqMonitoredPointRepository;
import com.kestrel.common.events.IndianRiverCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class AqMonitoredPointSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AqMonitoredPointSeeder.class);

    private final AqMonitoredPointRepository repository;

    public AqMonitoredPointSeeder(AqMonitoredPointRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        long currentCount = repository.count();
        if (currentCount < IndianRiverCatalog.ALL_45_STATIONS.size()) {
            log.info("Bootstrapping AQ monitored stations catalog (current count: {}, target: {})",
                    currentCount, IndianRiverCatalog.ALL_45_STATIONS.size());

            List<AqMonitoredPointEntity> toSave = new ArrayList<>();
            for (IndianRiverCatalog.RiverStation station : IndianRiverCatalog.ALL_45_STATIONS) {
                if (!repository.existsById(station.locationId())) {
                    toSave.add(new AqMonitoredPointEntity(
                            station.locationId(),
                            station.name(),
                            station.state(),
                            station.latitude(),
                            station.longitude(),
                            station.active()
                    ));
                }
            }

            if (!toSave.isEmpty()) {
                repository.saveAll(toSave);
                log.info("Bootstrapped {} AQ monitored points into DynamoDB", toSave.size());
            }
        } else {
            log.info("AQ monitored stations already seeded (count: {})", currentCount);
        }
    }
}

