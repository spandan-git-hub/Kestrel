package com.kestrel.aqingest.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.aqingest.infra.client.OpenMeteoAqClient;
import com.kestrel.aqingest.infra.entity.AqForecastRunEntity;
import com.kestrel.aqingest.infra.entity.AqHourlyValueEntity;
import com.kestrel.aqingest.infra.entity.AqMonitoredPointEntity;
import com.kestrel.aqingest.infra.publisher.AqSignalEventPublisher;
import com.kestrel.aqingest.infra.repository.inmemory.InMemoryAqRepositories.*;
import com.kestrel.common.core.budget.InMemoryCallBudgetTracker;
import com.kestrel.common.events.AqSignalPayload;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.EventObjectMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AqForecastServiceTest {

    private InMemoryAqMonitoredPointRepository pointRepository;
    private InMemoryAqForecastRunRepository runRepository;
    private InMemoryAqHourlyValueRepository hourlyValueRepository;
    private InMemoryAqFeedStateRepository feedStateRepository;
    private AqSignalEventPublisher signalPublisher;
    private OpenMeteoAqClient aqClient;
    private AqForecastService forecastService;
    private MockRestServiceServer mockServer;
    private final List<Object> capturedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        pointRepository = new InMemoryAqMonitoredPointRepository();
        runRepository = new InMemoryAqForecastRunRepository();
        hourlyValueRepository = new InMemoryAqHourlyValueRepository();
        feedStateRepository = new InMemoryAqFeedStateRepository();

        pointRepository.save(new AqMonitoredPointEntity(
                "yamuna-delhi", "Old Railway Bridge, Delhi", "Delhi",
                28.66, 77.24, true
        ));

        RestClient.Builder clientBuilder = RestClient.builder().baseUrl("https://air-quality-api.open-meteo.com/v1/air-quality");
        mockServer = MockRestServiceServer.bindTo(clientBuilder).build();
        RestClient restClient = clientBuilder.build();
        ObjectMapper objectMapper = EventObjectMapperFactory.createObjectMapper();
        InMemoryCallBudgetTracker budgetTracker = new InMemoryCallBudgetTracker(500, 8000);

        aqClient = new OpenMeteoAqClient(restClient, objectMapper, budgetTracker);

        capturedEvents.clear();
        ApplicationEventPublisher eventPublisher = capturedEvents::add;
        RestClient dummyEventBusClient = RestClient.builder().baseUrl("http://localhost:8083").build();
        signalPublisher = new AqSignalEventPublisher(
                eventPublisher,
                dummyEventBusClient,
                new org.springframework.beans.factory.support.StaticListableBeanFactory()
                        .getBeanProvider(com.kestrel.common.core.publisher.SnsEventPublisher.class)
        );

        forecastService = new AqForecastService(
                aqClient,
                pointRepository,
                runRepository,
                hourlyValueRepository,
                feedStateRepository,
                signalPublisher
        );
    }

    @Test
    @DisplayName("Syncs live AQ telemetry, calculates rolling averages, persists with 5-day TTL, and publishes AqSignal")
    void syncsLiveAqAndPublishesSignal() {
        // Build 24 hours of valid AQ telemetry
        StringBuilder timeBuilder = new StringBuilder();
        StringBuilder pm10Builder = new StringBuilder();
        StringBuilder pm25Builder = new StringBuilder();
        StringBuilder no2Builder = new StringBuilder();
        StringBuilder so2Builder = new StringBuilder();
        StringBuilder o3Builder = new StringBuilder();
        StringBuilder coBuilder = new StringBuilder();

        for (int h = 0; h < 24; h++) {
            if (h > 0) {
                timeBuilder.append(",");
                pm10Builder.append(",");
                pm25Builder.append(",");
                no2Builder.append(",");
                so2Builder.append(",");
                o3Builder.append(",");
                coBuilder.append(",");
            }
            timeBuilder.append(String.format("\"2026-10-31T%02d:00\"", h));
            pm10Builder.append(150.0);
            pm25Builder.append(85.0);
            no2Builder.append(45.0);
            so2Builder.append(15.0);
            o3Builder.append(35.0);
            coBuilder.append(1400.0);
        }

        String jsonResponse = String.format("""
                {
                  "latitude": 28.66,
                  "longitude": 77.24,
                  "timezone": "Asia/Kolkata",
                  "hourly": {
                    "time": [%s],
                    "pm10": [%s],
                    "pm2_5": [%s],
                    "nitrogen_dioxide": [%s],
                    "sulphur_dioxide": [%s],
                    "ozone": [%s],
                    "carbon_monoxide": [%s]
                  }
                }
                """,
                timeBuilder, pm10Builder, pm25Builder, no2Builder, so2Builder, o3Builder, coBuilder);

        mockServer.expect(requestTo(containsString("latitude=28.66")))
                .andExpect(requestTo(containsString("longitude=77.24")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<AqForecastService.AqSyncResult> resultOpt = forecastService.syncLive("yamuna-delhi");

        mockServer.verify();
        assertThat(resultOpt).isPresent();
        AqForecastService.AqSyncResult res = resultOpt.get();
        assertThat(res.success()).isTrue();
        assertThat(res.hourlyPointsProcessed()).isEqualTo(24);

        // Verify DynamoDB persistence in kestrel_aq_forecast_run with 5-day TTL
        Optional<AqForecastRunEntity> latestRun = runRepository.findLatestByLocationId("yamuna-delhi");
        assertThat(latestRun).isPresent();
        assertThat(latestRun.get().getTtl()).isGreaterThan(Instant.now().getEpochSecond() + (4L * 24 * 3600));
        assertThat(latestRun.get().getCurrentPm25_24h()).isEqualTo(85.0);
        assertThat(latestRun.get().getCurrentCo_8h_mgm3()).isEqualTo(1.4); // 1400 / 1000

        // Verify DynamoDB persistence in kestrel_aq_hourly_value with 5-day TTL
        List<AqHourlyValueEntity> hourly = hourlyValueRepository.findByLocationId("yamuna-delhi");
        assertThat(hourly).hasSize(24);
        assertThat(hourly.get(0).getTtl()).isGreaterThan(Instant.now().getEpochSecond() + (4L * 24 * 3600));

        // Verify feed state updated to HEALTHY
        assertThat(feedStateRepository.findByName("OPEN_METEO_AQ")).isPresent();
        assertThat(feedStateRepository.findByName("OPEN_METEO_AQ").get().getStatus()).isEqualTo("HEALTHY");

        // Verify AqSignal event published
        assertThat(capturedEvents).hasSize(1);
        Object rawEvent = capturedEvents.get(0);
        assertThat(rawEvent).isInstanceOf(EventEnvelope.class);
        @SuppressWarnings("unchecked")
        EventEnvelope<AqSignalPayload> envelope = (EventEnvelope<AqSignalPayload>) rawEvent;
        assertThat(envelope.eventType()).isEqualTo("AqSignal");
        assertThat(envelope.payload().locationId()).isEqualTo("yamuna-delhi");
        assertThat(envelope.payload().current().pm25_24h()).isEqualTo(85.0);
        assertThat(envelope.payload().current().co_8h_mgm3()).isEqualTo(1.4);
    }
}

