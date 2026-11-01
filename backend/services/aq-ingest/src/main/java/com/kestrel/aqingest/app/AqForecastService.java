package com.kestrel.aqingest.app;

import com.kestrel.aqingest.domain.RollingAverageCalculator;
import com.kestrel.aqingest.infra.client.OpenMeteoAqClient;
import com.kestrel.aqingest.infra.entity.AqFeedStateEntity;
import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.metrics.AqMetricsPublisher;
import com.kestrel.aqingest.infra.publisher.AqSignalEventPublisher;
import com.kestrel.aqingest.infra.repository.AqFeedStateRepository;
import com.kestrel.aqingest.infra.repository.AqForecastRunRepository;
import com.kestrel.aqingest.infra.repository.AqHourlyValueRepository;
import com.kestrel.aqingest.infra.repository.AqMonitoredPointRepository;
import com.kestrel.common.core.aqi.AqiCalculator;
import com.kestrel.common.events.AqSignalPayload;
import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.Pollutant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AqForecastService {

    private static final Logger log = LoggerFactory.getLogger(AqForecastService.class);
    private static final String FEED_NAME = "OPEN_METEO_AQ";
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final OpenMeteoAqClient aqClient;
    private final AqMonitoredPointRepository pointRepository;
    private final AqForecastRunRepository runRepository;
    private final AqHourlyValueRepository hourlyValueRepository;
    private final AqFeedStateRepository feedStateRepository;
    private final AqSignalEventPublisher signalPublisher;
    private final AqMetricsPublisher metricsPublisher;

    public AqForecastService(
            OpenMeteoAqClient aqClient,
            AqMonitoredPointRepository pointRepository,
            AqForecastRunRepository runRepository,
            AqHourlyValueRepository hourlyValueRepository,
            AqFeedStateRepository feedStateRepository,
            AqSignalEventPublisher signalPublisher) {
        this(aqClient, pointRepository, runRepository, hourlyValueRepository, feedStateRepository, signalPublisher, null);
    }

    @Autowired
    public AqForecastService(
            OpenMeteoAqClient aqClient,
            AqMonitoredPointRepository pointRepository,
            AqForecastRunRepository runRepository,
            AqHourlyValueRepository hourlyValueRepository,
            AqFeedStateRepository feedStateRepository,
            AqSignalEventPublisher signalPublisher,
            @Autowired(required = false) AqMetricsPublisher metricsPublisher) {
        this.aqClient = aqClient;
        this.pointRepository = pointRepository;
        this.runRepository = runRepository;
        this.hourlyValueRepository = hourlyValueRepository;
        this.feedStateRepository = feedStateRepository;
        this.signalPublisher = signalPublisher;
        this.metricsPublisher = metricsPublisher;
    }

    public Optional<AqSyncResult> syncLive(String locationId) {
        long startNanos = System.nanoTime();
        Optional<AqMonitoredPointEntity> pointOpt = pointRepository.findById(locationId);
        double latitude;
        double longitude;

        if (pointOpt.isPresent()) {
            AqMonitoredPointEntity point = pointOpt.get();
            latitude = point.getLatitude();
            longitude = point.getLongitude();
        } else {
            Optional<IndianRiverCatalog.RiverStation> stationOpt = IndianRiverCatalog.findById(locationId);
            if (stationOpt.isEmpty()) {
                log.warn("Cannot sync AQ for unknown locationId: {}", locationId);
                return Optional.empty();
            }
            IndianRiverCatalog.RiverStation station = stationOpt.get();
            latitude = station.latitude();
            longitude = station.longitude();
        }

        Optional<OpenMeteoAqClient.AqForecastResult> resultOpt = aqClient.fetchHourlyAq(latitude, longitude);

        if (resultOpt.isEmpty()) {
            recordFeedFailure();
            return Optional.empty();
        }

        recordFeedSuccess();
        OpenMeteoAqClient.AqForecastResult result = resultOpt.get();
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        LocalDateTime nowKolkata = LocalDateTime.ofInstant(now, IST_ZONE);
        LocalDate todayKolkata = nowKolkata.toLocalDate();

        // 1. Calculate trailing rolling averages:
        //    24-hour rolling mean for PM2.5, PM10, NO2, SO2 (valid >= 16h)
        //    8-hour rolling mean for O3, CO (valid >= 6h)
        List<RollingAverageCalculator.ProcessedHourlyAq> processedHourly =
                RollingAverageCalculator.processHourlyData(result.rawHourly());

        // 2. Current rolling window ending at latest hour <= now
        AqSignalPayload.CurrentPollutants current =
                RollingAverageCalculator.extractCurrentPollutants(processedHourly, nowKolkata);

        // 3. Peak rolling windows for each forecast day
        List<AqSignalPayload.ForecastDayPeak> forecastPeaks =
                RollingAverageCalculator.extractForecastPeaks(processedHourly, todayKolkata);

        // 4. Persist hourly records with 5-day TTL in DynamoDB table kestrel_aq_hourly_value
        List<AqHourlyValueEntity> hourlyEntities = new ArrayList<>(processedHourly.size());
        for (RollingAverageCalculator.ProcessedHourlyAq item : processedHourly) {
            hourlyEntities.add(new AqHourlyValueEntity(
                    locationId,
                    item.time(),
                    item.pm25(),
                    item.pm10(),
                    item.no2(),
                    item.so2(),
                    item.o3(),
                    item.coMgm3(),
                    item.pm25_24h(),
                    item.pm10_24h(),
                    item.no2_24h(),
                    item.so2_24h(),
                    item.o3_8h(),
                    item.co_8h_mgm3()
            ));
        }
        hourlyValueRepository.saveAll(hourlyEntities);

        // 5. Persist forecast run with 5-day TTL in DynamoDB table kestrel_aq_forecast_run
        AqForecastRunEntity runEntity = new AqForecastRunEntity(
                locationId,
                runId,
                result.fetchedAt(),
                result.fetchedAt(),
                current.pm25_24h(),
                current.pm10_24h(),
                current.no2_24h(),
                current.so2_24h(),
                current.o3_8h(),
                current.co_8h_mgm3(),
                current.validHours(),
                forecastPeaks.size()
        );
        runRepository.save(runEntity);

        // 6. Emit real AqSignal event envelope to SNS topic kestrel-signals (x-event-type=AqSignal)
        AqSignalPayload signalPayload = new AqSignalPayload(
                locationId,
                runId,
                result.fetchedAt(),
                result.fetchedAt(),
                current,
                forecastPeaks
        );
        signalPublisher.publish(signalPayload);

        double durationSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        if (metricsPublisher != null) {
            metricsPublisher.recordIngestDuration(durationSeconds);

            int peakAqiAcrossForecast = 0;
            for (AqSignalPayload.ForecastDayPeak peak : forecastPeaks) {
                try {
                    Map<Pollutant, Double> map = new EnumMap<>(Pollutant.class);
                    map.put(Pollutant.PM25, peak.pm25_24h());
                    map.put(Pollutant.PM10, peak.pm10_24h());
                    map.put(Pollutant.NO2, peak.no2_24h());
                    map.put(Pollutant.SO2, peak.so2_24h());
                    map.put(Pollutant.O3, peak.o3_8h());
                    map.put(Pollutant.CO, peak.co_8h_mgm3());
                    int aqi = AqiCalculator.calculateAqi(map);
                    if (aqi > peakAqiAcrossForecast) {
                        peakAqiAcrossForecast = aqi;
                    }
                } catch (Exception ignored) {}
            }
            if (peakAqiAcrossForecast > 0) {
                metricsPublisher.recordPeakAqi(peakAqiAcrossForecast);
            }
        }

        log.info("Synced AQ forecast for location={}: {} hours, {} forecast peaks, current PM2.5_24h={}, duration={}",
                locationId, processedHourly.size(), forecastPeaks.size(), current.pm25_24h(),
                String.format("%.3f", durationSeconds));

        return Optional.of(new AqSyncResult(
                locationId,
                runId,
                processedHourly.size(),
                forecastPeaks.size(),
                current.pm25_24h(),
                true,
                "AQ telemetry successfully synced, rolling averages calculated, and AqSignal emitted."
        ));
    }

    public List<AqSignalPayload.ForecastDayPeak> getForecastPeaks(String locationId) {
        List<AqHourlyValueEntity> hourly = hourlyValueRepository.findByLocationId(locationId);
        if (hourly.isEmpty()) {
            return List.of();
        }
        List<RollingAverageCalculator.ProcessedHourlyAq> processed = hourly.stream()
                .map(h -> new RollingAverageCalculator.ProcessedHourlyAq(
                        h.getTime(),
                        h.getPm10(),
                        h.getPm25(),
                        h.getNo2(),
                        h.getSo2(),
                        h.getOzone(),
                        h.getCoMgm3(),
                        h.getPm25_24h(),
                        h.getPm10_24h(),
                        h.getNo2_24h(),
                        h.getSo2_24h(),
                        h.getO3_8h(),
                        h.getCo_8h_mgm3(),
                        24
                ))
                .toList();

        LocalDate today = LocalDate.now(IST_ZONE);
        return RollingAverageCalculator.extractForecastPeaks(processed, today);
    }

    public int syncLiveAll() {
        List<AqMonitoredPointEntity> activePoints = pointRepository.findByActiveTrue();
        int synced = 0;

        if (activePoints.isEmpty()) {
            // Fallback to all 45 stations
            for (IndianRiverCatalog.RiverStation station : IndianRiverCatalog.ALL_45_STATIONS) {
                Optional<AqSyncResult> resultOpt = syncLive(station.locationId());
                if (resultOpt.isPresent()) {
                    synced++;
                }
            }
        } else {
            for (AqMonitoredPointEntity pt : activePoints) {
                Optional<AqSyncResult> resultOpt = syncLive(pt.getLocationId());
                if (resultOpt.isPresent()) {
                    synced++;
                }
            }
        }
        return synced;
    }

    public List<AqHourlyValueEntity> getHourlyValues(String locationId) {
        return hourlyValueRepository.findByLocationId(locationId);
    }

    public Optional<AqForecastRunEntity> getLatestRun(String locationId) {
        return runRepository.findLatestByLocationId(locationId);
    }

    public Optional<AqFeedStateEntity> getFeedStatus() {
        return feedStateRepository.findByName(FEED_NAME);
    }

    private void recordFeedSuccess() {
        try {
            AqFeedStateEntity state = feedStateRepository.findByName(FEED_NAME)
                    .orElse(new AqFeedStateEntity(FEED_NAME, "HEALTHY", null, null, 0));
            state.setStatus("HEALTHY");
            state.setLastSuccessAt(Instant.now());
            state.setConsecutiveFailures(0);
            feedStateRepository.save(state);
        } catch (Exception e) {
            log.warn("Could not update AQ feed state success: {}", e.getMessage());
        }
    }

    private void recordFeedFailure() {
        try {
            AqFeedStateEntity state = feedStateRepository.findByName(FEED_NAME)
                    .orElse(new AqFeedStateEntity(FEED_NAME, "HEALTHY", null, null, 0));
            int fails = state.getConsecutiveFailures() + 1;
            state.setConsecutiveFailures(fails);
            state.setLastFailureAt(Instant.now());
            state.setStatus(fails >= 3 ? "DOWN" : "DEGRADED");
            feedStateRepository.save(state);
        } catch (Exception e) {
            log.warn("Could not update AQ feed state failure: {}", e.getMessage());
        }
    }

    public record AqSyncResult(
            String locationId,
            String runId,
            int hourlyPointsProcessed,
            int forecastPeaksCount,
            double currentPm25_24h,
            boolean success,
            String message
    ) {}
}
