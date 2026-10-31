package com.kestrel.aqingest.infra.lambda;

import com.kestrel.aqingest.app.AqForecastService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AqIngestScheduledFunctionTest {

    @Test
    @DisplayName("dailyAqIngest function triggers syncLiveAll and returns sync count")
    void dailyAqIngestTriggersSync() {
        AqForecastService stubService = new AqForecastService(
                null, null, null, null, null, null
        ) {
            @Override
            public int syncLiveAll() {
                return 45;
            }
        };

        AqIngestScheduledFunction functionConfig = new AqIngestScheduledFunction();
        var ingestFunction = functionConfig.dailyAqIngest(stubService);

        Map<String, Object> eventBridgeEvent = Map.of(
                "source", "aws.events",
                "detail-type", "Scheduled Event"
        );

        AqIngestScheduledFunction.ScheduledAqIngestResponse response = ingestFunction.apply(eventBridgeEvent);

        assertThat(response.syncedStationsCount()).isEqualTo(45);
        assertThat(response.message()).contains("EventBridge scheduled");
    }
}

