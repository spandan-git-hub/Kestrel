package com.kestrel.heatingest.infra.lambda;

import com.kestrel.heatingest.app.HeatForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Function;

/**
 * EventBridge Scheduler Lambda Handler for automated twice-daily Open-Meteo
 * weather forecast ingestion across Indian monitoring locations.
 * Triggered at 04:15 UTC and 16:15 UTC to ensure heat advisory readiness before 09:00 AM IST.
 */
@Configuration
public class HeatIngestScheduledFunction {

    private static final Logger log = LoggerFactory.getLogger(HeatIngestScheduledFunction.class);

    @Bean(name = {"dailyHeatIngest", "dailyHeatSchedule"})
    public Function<Map<String, Object>, ScheduledHeatIngestResponse> dailyHeatIngest(HeatForecastService forecastService) {
        return event -> {
            Object source = event != null ? event.get("source") : "manual";
            Object detailType = event != null ? event.get("detail-type") : "Manual Trigger";
            log.info("Received EventBridge Scheduled Ingestion Event for Heat: source={}, detail-type={}", source, detailType);

            int syncedCount = forecastService.syncLiveAll();
            log.info("Completed EventBridge scheduled forecast synchronization for {} heat locations", syncedCount);

            return new ScheduledHeatIngestResponse(
                    "EventBridge scheduled Open-Meteo weather forecast sync completed successfully.",
                    syncedCount
            );
        };
    }

    public record ScheduledHeatIngestResponse(
            String message,
            int syncedStationsCount
    ) {}
}

