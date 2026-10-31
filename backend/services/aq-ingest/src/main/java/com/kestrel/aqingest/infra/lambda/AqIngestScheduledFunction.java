package com.kestrel.aqingest.infra.lambda;

import com.kestrel.aqingest.app.AqForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Function;

/**
 * EventBridge Scheduler Lambda Handler for automated twice-daily Open-Meteo
 * Air Quality telemetry ingestion across Indian monitoring locations.
 * Triggered at 02:20 UTC and 14:20 UTC every 12 hours.
 */
@Configuration
public class AqIngestScheduledFunction {

    private static final Logger log = LoggerFactory.getLogger(AqIngestScheduledFunction.class);

    @Bean(name = {"dailyAqIngest", "dailyAqSchedule"})
    public Function<Map<String, Object>, ScheduledAqIngestResponse> dailyAqIngest(AqForecastService forecastService) {
        return event -> {
            Object source = event != null ? event.get("source") : "manual";
            Object detailType = event != null ? event.get("detail-type") : "Manual Trigger";
            log.info("Received EventBridge Scheduled Ingestion Event for Air Quality: source={}, detail-type={}", source, detailType);

            int syncedCount = forecastService.syncLiveAll();
            log.info("Completed EventBridge scheduled forecast synchronization for {} AQ locations", syncedCount);

            return new ScheduledAqIngestResponse(
                    "EventBridge scheduled Open-Meteo AQ forecast sync completed successfully.",
                    syncedCount
            );
        };
    }

    public record ScheduledAqIngestResponse(
            String message,
            int syncedStationsCount
    ) {}
}

