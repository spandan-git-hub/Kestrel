package com.kestrel.heatingest.app;

import com.kestrel.common.events.HeatSignalPayload;
import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.RegionType;
import com.kestrel.heatingest.infra.client.OpenMeteoWeatherClient;
import com.kestrel.heatingest.infra.entity.HeatFeedStateEntity;
import com.kestrel.heatingest.infra.entity.HeatForecastDayEntity;
import com.kestrel.heatingest.infra.entity.HeatForecastRunEntity;
import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;
import com.kestrel.heatingest.infra.publisher.HeatSignalEventPublisher;
import com.kestrel.heatingest.infra.repository.HeatFeedStateRepository;
import com.kestrel.heatingest.infra.repository.HeatForecastDayRepository;
import com.kestrel.heatingest.infra.repository.HeatForecastRunRepository;
import com.kestrel.heatingest.infra.repository.HeatMonitoredPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HeatForecastService {

    private static final Logger log = LoggerFactory.getLogger(HeatForecastService.class);
    private static final String FEED_NAME = "OPEN_METEO_WEATHER";

    private final OpenMeteoWeatherClient weatherClient;
    private final HeatMonitoredPointRepository pointRepository;
    private final HeatForecastRunRepository runRepository;
    private final HeatForecastDayRepository dayRepository;
    private final HeatNormalSeeder normalSeeder;
    private final HeatFeedStateRepository feedStateRepository;
    private final HeatSignalEventPublisher signalPublisher;

    public HeatForecastService(
            OpenMeteoWeatherClient weatherClient,
            HeatMonitoredPointRepository pointRepository,
            HeatForecastRunRepository runRepository,
            HeatForecastDayRepository dayRepository,
            HeatNormalSeeder normalSeeder,
            HeatFeedStateRepository feedStateRepository,
            HeatSignalEventPublisher signalPublisher) {
        this.weatherClient = weatherClient;
        this.pointRepository = pointRepository;
        this.runRepository = runRepository;
        this.dayRepository = dayRepository;
        this.normalSeeder = normalSeeder;
        this.feedStateRepository = feedStateRepository;
        this.signalPublisher = signalPublisher;
    }

    public Optional<HeatSyncResult> syncLive(String locationId) {
        Optional<HeatMonitoredPointEntity> pointOpt = pointRepository.findById(locationId);
        double latitude;
        double longitude;
        RegionType regionType;

        if (pointOpt.isPresent()) {
            HeatMonitoredPointEntity point = pointOpt.get();
            latitude = point.getLatitude();
            longitude = point.getLongitude();
            regionType = point.getRegionType();
        } else {
            Optional<IndianRiverCatalog.RiverStation> stationOpt = IndianRiverCatalog.findById(locationId);
            if (stationOpt.isEmpty()) {
                log.warn("Cannot sync heat for unknown locationId: {}", locationId);
                return Optional.empty();
            }
            IndianRiverCatalog.RiverStation station = stationOpt.get();
            latitude = station.latitude();
            longitude = station.longitude();
            regionType = IndianRiverCatalog.getRegionType(locationId);
        }

        Optional<OpenMeteoWeatherClient.WeatherForecastResult> forecastOpt =
                weatherClient.fetch7DayForecast(latitude, longitude);

        if (forecastOpt.isEmpty()) {
            recordFeedFailure();
            return Optional.empty();
        }

        recordFeedSuccess();
        OpenMeteoWeatherClient.WeatherForecastResult result = forecastOpt.get();
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        List<HeatSignalPayload.HeatDayStats> signalDays = new ArrayList<>();
        List<HeatForecastDayEntity> dayEntities = new ArrayList<>();
        double peakDeparture = -999.0;

        for (OpenMeteoWeatherClient.DailyWeatherRecord rec : result.dailyRecords()) {
            int doy = rec.date().getDayOfYear();
            double normalTmax = normalSeeder.getNormal(locationId, doy);
            double departure = Math.round((rec.temperature2mMax() - normalTmax) * 10.0) / 10.0;

            if (departure > peakDeparture) {
                peakDeparture = departure;
            }

            HeatSignalPayload.HeatDayStats dayStat = new HeatSignalPayload.HeatDayStats(
                    rec.date(),
                    rec.temperature2mMax(),
                    rec.apparentTemperatureMax(),
                    rec.relativeHumidity2mMax(),
                    normalTmax,
                    departure
            );
            signalDays.add(dayStat);

            dayEntities.add(new HeatForecastDayEntity(
                    locationId,
                    rec.date().toString(),
                    rec.temperature2mMax(),
                    rec.apparentTemperatureMax(),
                    rec.relativeHumidity2mMax(),
                    normalTmax,
                    departure
            ));
        }

        // Persist forecast days with 30-day TTL in DynamoDB
        dayRepository.saveAll(dayEntities);

        // Persist forecast run with 30-day TTL in DynamoDB
        HeatForecastRunEntity runEntity = new HeatForecastRunEntity(
                locationId,
                runId,
                result.fetchedAt(),
                result.fetchedAt(),
                signalDays.size(),
                peakDeparture
        );
        runRepository.save(runEntity);

        // Publish HeatSignal event to SNS topic kestrel-signals (x-event-type=HeatSignal)
        HeatSignalPayload signalPayload = new HeatSignalPayload(
                locationId,
                runId,
                result.fetchedAt(),
                result.fetchedAt(),
                regionType,
                signalDays
        );
        signalPublisher.publish(signalPayload);

        log.info("Synced heat forecast for location={}: {} days, peakDeparture={}",
                locationId, signalDays.size(), peakDeparture);

        return Optional.of(new HeatSyncResult(
                locationId,
                runId,
                signalDays.size(),
                peakDeparture,
                true,
                "Weather forecast successfully synced and published."
        ));
    }

    public int syncLiveAll() {
        List<HeatMonitoredPointEntity> activePoints = pointRepository.findByActiveTrue();
        if (activePoints.isEmpty()) {
            // Fallback to all 45 stations
            int count = 0;
            for (IndianRiverCatalog.RiverStation station : IndianRiverCatalog.ALL_45_STATIONS) {
                if (syncLive(station.locationId()).isPresent()) {
                    count++;
                }
            }
            return count;
        }

        int synced = 0;
        for (HeatMonitoredPointEntity pt : activePoints) {
            if (syncLive(pt.getLocationId()).isPresent()) {
                synced++;
            }
        }
        return synced;
    }

    public List<HeatForecastDayEntity> getForecastDays(String locationId) {
        return dayRepository.findByLocationId(locationId);
    }

    public Optional<HeatForecastRunEntity> getLatestRun(String locationId) {
        return runRepository.findLatestByLocationId(locationId);
    }

    public Optional<HeatFeedStateEntity> getFeedStatus() {
        return feedStateRepository.findByName(FEED_NAME);
    }

    private void recordFeedSuccess() {
        HeatFeedStateEntity entity = feedStateRepository.findByName(FEED_NAME)
                .orElse(new HeatFeedStateEntity(FEED_NAME, "HEALTHY", Instant.now(), null, 0));
        entity.setStatus("HEALTHY");
        entity.setLastSuccessAt(Instant.now());
        entity.setConsecutiveFailures(0);
        entity.setUpdatedAt(Instant.now());
        feedStateRepository.save(entity);
    }

    private void recordFeedFailure() {
        HeatFeedStateEntity entity = feedStateRepository.findByName(FEED_NAME)
                .orElse(new HeatFeedStateEntity(FEED_NAME, "DEGRADED", null, Instant.now(), 0));
        int failures = entity.getConsecutiveFailures() + 1;
        entity.setConsecutiveFailures(failures);
        entity.setLastFailureAt(Instant.now());
        entity.setStatus(failures >= 3 ? "DOWN" : "DEGRADED");
        entity.setUpdatedAt(Instant.now());
        feedStateRepository.save(entity);
    }

    public record HeatSyncResult(
            String locationId,
            String runId,
            int daysCount,
            double peakDeparture,
            boolean success,
            String message
    ) {}
}

