package com.kestrel.heatingest.infra.lambda;

import com.kestrel.heatingest.app.HeatForecastService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeatIngestScheduledFunctionTest {

    @Test
    @DisplayName("dailyHeatIngest function triggers syncLiveAll and returns sync count")
    void dailyHeatIngestTriggersSync() {
        HeatForecastService stubService = new HeatForecastService(
                null, null, null, null, null, null, null
        ) {
            @Override
            public int syncLiveAll() {
                return 45;
            }
        };

        HeatIngestScheduledFunction functionConfig = new HeatIngestScheduledFunction();
        var ingestFunction = functionConfig.dailyHeatIngest(stubService);

        Map<String, Object> eventBridgeEvent = Map.of(
                "source", "aws.events",
                "detail-type", "Scheduled Event"
        );

        HeatIngestScheduledFunction.ScheduledHeatIngestResponse response = ingestFunction.apply(eventBridgeEvent);

        assertThat(response.syncedStationsCount()).isEqualTo(45);
        assertThat(response.message()).contains("EventBridge scheduled");
    }
}
