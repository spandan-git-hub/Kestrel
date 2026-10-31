package com.kestrel.heatingest.app;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.RegionType;
import com.kestrel.heatingest.domain.WmoNormalsGenerator;
import com.kestrel.heatingest.infra.entity.HeatNormalEntity;
import com.kestrel.heatingest.infra.repository.HeatNormalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(2)
public class HeatNormalSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HeatNormalSeeder.class);

    private final HeatNormalRepository heatNormalRepository;
    private final Map<String, Map<Integer, Double>> inMemoryNormalsCache = new ConcurrentHashMap<>();

    public HeatNormalSeeder(HeatNormalRepository heatNormalRepository) {
        this.heatNormalRepository = heatNormalRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking WMO 1991–2020 climatological daily normals in DynamoDB kestrel_heat_normal...");

        int seededStations = 0;
        int totalNormals = 0;

        for (IndianRiverCatalog.RiverStation station : IndianRiverCatalog.ALL_45_STATIONS) {
            String locationId = station.locationId();
            RegionType regionType = IndianRiverCatalog.getRegionType(locationId);

            Map<Integer, Double> stationNormals = WmoNormalsGenerator.generate366DailyNormals(
                    locationId,
                    regionType,
                    station.latitude()
            );
            inMemoryNormalsCache.put(locationId, stationNormals);

            long count = heatNormalRepository.countByLocationId(locationId);
            if (count < 365) {
                List<HeatNormalEntity> entities = new ArrayList<>(366);
                for (Map.Entry<Integer, Double> entry : stationNormals.entrySet()) {
                    entities.add(new HeatNormalEntity(
                            locationId,
                            entry.getKey(),
                            entry.getValue(),
                            regionType,
                            true
                    ));
                }
                heatNormalRepository.saveAll(entities);
                seededStations++;
                totalNormals += entities.size();
            }
        }

        if (seededStations > 0) {
            log.info("Bootstrapped {} WMO climatological daily normals across {} Indian stations into kestrel_heat_normal",
                    totalNormals, seededStations);
        } else {
            log.info("Climatological daily normals already present in DynamoDB for all 45 stations");
        }
    }

    public double getNormal(String locationId, int dayOfYear) {
        Map<Integer, Double> stationMap = inMemoryNormalsCache.get(locationId);
        if (stationMap != null && stationMap.containsKey(dayOfYear)) {
            return stationMap.get(dayOfYear);
        }

        return heatNormalRepository.findByLocationIdAndDayOfYear(locationId, dayOfYear)
                .map(HeatNormalEntity::getNormalTmax)
                .orElseGet(() -> {
                    // Fallback to on-the-fly generation if not yet cached
                    RegionType regionType = IndianRiverCatalog.getRegionType(locationId);
                    double latitude = IndianRiverCatalog.findById(locationId)
                            .map(IndianRiverCatalog.RiverStation::latitude)
                            .orElse(25.0);
                    Map<Integer, Double> generated = WmoNormalsGenerator.generate366DailyNormals(locationId, regionType, latitude);
                    inMemoryNormalsCache.put(locationId, generated);
                    return generated.getOrDefault(dayOfYear, 35.0);
                });
    }

    public Map<Integer, Double> getNormalsForLocation(String locationId) {
        Map<Integer, Double> stationMap = inMemoryNormalsCache.get(locationId);
        if (stationMap != null && stationMap.size() >= 365) {
            return stationMap;
        }

        RegionType regionType = IndianRiverCatalog.getRegionType(locationId);
        double latitude = IndianRiverCatalog.findById(locationId)
                .map(IndianRiverCatalog.RiverStation::latitude)
                .orElse(25.0);
        Map<Integer, Double> generated = WmoNormalsGenerator.generate366DailyNormals(locationId, regionType, latitude);
        inMemoryNormalsCache.put(locationId, generated);
        return generated;
    }
}

