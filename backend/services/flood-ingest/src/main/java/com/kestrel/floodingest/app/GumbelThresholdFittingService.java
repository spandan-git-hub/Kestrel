package com.kestrel.floodingest.app;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.IndianRiverCatalog.RiverStation;
import com.kestrel.floodingest.domain.GumbelDistribution;
import com.kestrel.floodingest.domain.GumbelDistribution.GumbelFitSummary;
import com.kestrel.floodingest.domain.GumbelDistribution.ReturnPeriodThresholds;
import com.kestrel.floodingest.infra.client.OpenMeteoClient;
import com.kestrel.floodingest.infra.client.OpenMeteoClient.HistoricalDischargeResult;
import com.kestrel.floodingest.infra.entity.FloodThresholdEntity;
import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import com.kestrel.floodingest.infra.repository.FloodThresholdRepository;
import com.kestrel.floodingest.infra.repository.MonitoredPointReplicaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GumbelThresholdFittingService {

    private static final Logger log = LoggerFactory.getLogger(GumbelThresholdFittingService.class);

    private final OpenMeteoClient openMeteoClient;
    private final MonitoredPointReplicaRepository replicaRepository;
    private final FloodThresholdRepository thresholdRepository;

    public GumbelThresholdFittingService(
            OpenMeteoClient openMeteoClient,
            MonitoredPointReplicaRepository replicaRepository,
            FloodThresholdRepository thresholdRepository) {
        this.openMeteoClient = openMeteoClient;
        this.replicaRepository = replicaRepository;
        this.thresholdRepository = thresholdRepository;
    }

    public ThresholdFitResult fitThresholdsForLocation(String locationId) {
        MonitoredPointReplica replica = replicaRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown monitored point: " + locationId));

        log.info("Starting Gumbel threshold fitting for station {} ({}, {})",
                locationId, replica.getName(), replica.getRiverName());

        Optional<HistoricalDischargeResult> histOpt = openMeteoClient.fetchHistoricalDailyDischarge(
                replica.getLatitude(),
                replica.getLongitude()
        );

        if (histOpt.isEmpty() || histOpt.get().annualMaxima().size() < 2) {
            log.warn("Historical discharge data unavailable or insufficient (<2 years) for {}. Using catalog baselines.",
                    locationId);
            return fallbackToCatalog(replica, "Insufficient historical observations (<2 annual maxima)");
        }

        HistoricalDischargeResult hist = histOpt.get();
        List<Double> annualMaxima = new ArrayList<>(hist.annualMaxima().values());

        try {
            GumbelDistribution fittedDist = GumbelDistribution.fit(annualMaxima);
            GumbelFitSummary summary = fittedDist.getFitSummary();
            ReturnPeriodThresholds thresholds = summary.thresholds();

            Instant now = Instant.now();

            // Update MonitoredPointReplica in DynamoDB
            replica.setThreshold2yr(thresholds.t2());
            replica.setThreshold5yr(thresholds.t5());
            replica.setThreshold20yr(thresholds.t20());
            replica.setGumbelMu(summary.locationMu());
            replica.setGumbelBeta(summary.scaleBeta());
            replica.setGumbelSampleYears(summary.sampleCount());
            replica.setThresholdsFittedAt(now);
            replicaRepository.save(replica);

            // Persist full parameters to FloodThresholdRepository (kestrel_flood_threshold)
            FloodThresholdEntity thresholdEntity = new FloodThresholdEntity(
                    replica.getLocationId(),
                    replica.getRiverName(),
                    replica.getBasin() != null ? replica.getBasin() : "India",
                    replica.getLatitude(),
                    replica.getLongitude(),
                    summary.mean(),
                    summary.stdDev(),
                    summary.locationMu(),
                    summary.scaleBeta(),
                    thresholds.t2(),
                    thresholds.t5(),
                    thresholds.t20(),
                    summary.sampleCount(),
                    now
            );
            thresholdRepository.save(thresholdEntity);

            log.info("Successfully fitted Gumbel thresholds for {}: T2={}, T5={}, T20={} (samples={}, mu={}, beta={})",
                    locationId, thresholds.t2(), thresholds.t5(), thresholds.t20(),
                    summary.sampleCount(), summary.locationMu(), summary.scaleBeta());

            return new ThresholdFitResult(
                    locationId,
                    replica.getName(),
                    replica.getRiverName(),
                    summary.mean(),
                    summary.stdDev(),
                    summary.locationMu(),
                    summary.scaleBeta(),
                    thresholds.t2(),
                    thresholds.t5(),
                    thresholds.t20(),
                    summary.sampleCount(),
                    true,
                    "Fitted Gumbel distribution from " + summary.sampleCount() + " historical annual maxima (1984–present)"
            );

        } catch (Exception ex) {
            log.error("Failed to fit Gumbel distribution for {}: {}", locationId, ex.getMessage(), ex);
            return fallbackToCatalog(replica, "Fitting error: " + ex.getMessage());
        }
    }

    public Map<String, ThresholdFitResult> fitThresholdsForAllLocations() {
        List<MonitoredPointReplica> activeLocations = replicaRepository.findByActiveTrue();
        Map<String, ThresholdFitResult> results = new LinkedHashMap<>();
        for (MonitoredPointReplica replica : activeLocations) {
            try {
                ThresholdFitResult result = fitThresholdsForLocation(replica.getLocationId());
                results.put(replica.getLocationId(), result);
            } catch (Exception ex) {
                log.error("Failed to fit thresholds for {}: {}", replica.getLocationId(), ex.getMessage());
            }
        }
        return results;
    }

    public Optional<FloodThresholdEntity> getThreshold(String locationId) {
        return thresholdRepository.findById(locationId);
    }

    public List<FloodThresholdEntity> getAllThresholds() {
        return thresholdRepository.findAll();
    }

    private ThresholdFitResult fallbackToCatalog(MonitoredPointReplica replica, String reason) {
        Optional<RiverStation> stationOpt = IndianRiverCatalog.findById(replica.getLocationId());
        double t2 = stationOpt.map(RiverStation::threshold2yr).orElse(replica.getThreshold2yr());
        double t5 = stationOpt.map(RiverStation::threshold5yr).orElse(replica.getThreshold5yr());
        double t20 = stationOpt.map(RiverStation::threshold20yr).orElse(replica.getThreshold20yr());

        return new ThresholdFitResult(
                replica.getLocationId(),
                replica.getName(),
                replica.getRiverName(),
                0.0,
                0.0,
                0.0,
                0.0,
                t2,
                t5,
                t20,
                0,
                false,
                "Catalog baseline applied (" + reason + ")"
        );
    }

    public record ThresholdFitResult(
            String locationId,
            String locationName,
            String riverName,
            double mean,
            double stdDev,
            double locationMu,
            double scaleBeta,
            double threshold2yr,
            double threshold5yr,
            double threshold20yr,
            int sampleCount,
            boolean success,
            String message
    ) {}
}

