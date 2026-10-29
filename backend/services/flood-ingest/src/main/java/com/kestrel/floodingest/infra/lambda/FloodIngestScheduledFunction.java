package com.kestrel.floodingest.infra.lambda;

import com.kestrel.floodingest.api.dto.SyncLiveResponse;
import com.kestrel.floodingest.app.FloodForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Function;

/**
 * EventBridge Scheduler Lambda Handler for automated daily/periodic Open-Meteo
 * river forecast ingestion across all 45 Indian river monitoring stations.
 */
@Configuration
public class FloodIngestScheduledFunction {

    private static final Logger log = LoggerFactory.getLogger(FloodIngestScheduledFunction.class);

    @Bean(name = {"dailyFloodIngest", "dailyFloodSchedule"})
    public Function<Map<String, Object>, SyncLiveResponse> dailyFloodIngest(FloodForecastService forecastService) {
        return event -> {
            Object source = event != null ? event.get("source") : "manual";
            Object detailType = event != null ? event.get("detail-type") : "Manual Trigger";
            log.info("Received EventBridge Scheduled Ingestion Event: source={}, detail-type={}", source, detailType);

            int syncedCount = forecastService.syncLiveAll();
            log.info("Completed EventBridge scheduled forecast synchronization for {} Indian river stations", syncedCount);

            return new SyncLiveResponse(
                    "EventBridge scheduled Open-Meteo ensemble forecast sync completed successfully.",
                    syncedCount
            );
        };
    }
}

