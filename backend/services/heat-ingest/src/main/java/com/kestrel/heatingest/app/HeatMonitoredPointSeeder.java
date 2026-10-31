package com.kestrel.heatingest.app;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.RegionType;
import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;
import com.kestrel.heatingest.infra.repository.HeatMonitoredPointRepository;
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
public class HeatMonitoredPointSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HeatMonitoredPointSeeder.class);

    private final HeatMonitoredPointRepository repository;

    public HeatMonitoredPointSeeder(HeatMonitoredPointRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        long currentCount = repository.count();
        if (currentCount < IndianRiverCatalog.ALL_45_STATIONS.size()) {
            log.info("Bootstrapping heat monitored stations catalog (current count: {}, target: {})",
                    currentCount, IndianRiverCatalog.ALL_45_STATIONS.size());

            List<HeatMonitoredPointEntity> toSave = new ArrayList<>();
            for (IndianRiverCatalog.RiverStation station : IndianRiverCatalog.ALL_45_STATIONS) {
                if (!repository.existsById(station.locationId())) {
                    RegionType regionType = IndianRiverCatalog.getRegionType(station.locationId());
                    toSave.add(new HeatMonitoredPointEntity(
                            station.locationId(),
                            station.name(),
                            station.state(),
                            station.latitude(),
                            station.longitude(),
                            regionType,
                            station.active()
                    ));
                }
            }

            if (!toSave.isEmpty()) {
                repository.saveAll(toSave);
                log.info("Bootstrapped {} heat monitored points into DynamoDB", toSave.size());
            }
        } else {
            log.info("Heat monitored stations already seeded (count: {})", currentCount);
        }
    }
}

