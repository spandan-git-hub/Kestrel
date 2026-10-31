package com.kestrel.heatingest.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.core.budget.InMemoryCallBudgetTracker;
import com.kestrel.common.events.EventEnvelope;
import com.kestrel.common.events.EventObjectMapperFactory;
import com.kestrel.common.events.HeatSignalPayload;
import com.kestrel.common.events.RegionType;
import com.kestrel.heatingest.infra.client.OpenMeteoWeatherClient;
import com.kestrel.heatingest.infra.entity.HeatForecastDayEntity;
import com.kestrel.heatingest.infra.entity.HeatMonitoredPointEntity;
import com.kestrel.heatingest.infra.publisher.HeatSignalEventPublisher;
import com.kestrel.heatingest.infra.repository.inmemory.InMemoryHeatRepositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HeatForecastServiceTest {

    private InMemoryHeatMonitoredPointRepository pointRepository;
    private InMemoryHeatForecastRunRepository runRepository;
    private InMemoryHeatForecastDayRepository dayRepository;
    private InMemoryHeatNormalRepository normalRepository;
    private InMemoryHeatFeedStateRepository feedStateRepository;
    private HeatNormalSeeder normalSeeder;
    private HeatSignalEventPublisher signalPublisher;
    private OpenMeteoWeatherClient weatherClient;
    private HeatForecastService forecastService;
    private MockRestServiceServer mockServer;
    private final List<Object> capturedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        pointRepository = new InMemoryHeatMonitoredPointRepository();
        runRepository = new InMemoryHeatForecastRunRepository();
        dayRepository = new InMemoryHeatForecastDayRepository();
        normalRepository = new InMemoryHeatNormalRepository();
        feedStateRepository = new InMemoryHeatFeedStateRepository();

        normalSeeder = new HeatNormalSeeder(normalRepository);
        normalSeeder.run(null); // Seeds all 45 stations

        pointRepository.save(new HeatMonitoredPointEntity(
                "yamuna-delhi", "Old Railway Bridge, Delhi", "Delhi",
                28.66, 77.24, RegionType.PLAINS, true
        ));

        RestClient.Builder clientBuilder = RestClient.builder().baseUrl("https://api.open-meteo.com/v1/forecast");
        mockServer = MockRestServiceServer.bindTo(clientBuilder).build();
        RestClient restClient = clientBuilder.build();
        ObjectMapper objectMapper = EventObjectMapperFactory.createObjectMapper();
        InMemoryCallBudgetTracker budgetTracker = new InMemoryCallBudgetTracker(500, 8000);

        weatherClient = new OpenMeteoWeatherClient(restClient, objectMapper, budgetTracker);

        capturedEvents.clear();
        ApplicationEventPublisher eventPublisher = capturedEvents::add;
        RestClient dummyEventBusClient = RestClient.builder().baseUrl("http://localhost:8083").build();
        signalPublisher = new HeatSignalEventPublisher(
                eventPublisher,
                dummyEventBusClient,
                new org.springframework.beans.factory.support.StaticListableBeanFactory()
                        .getBeanProvider(com.kestrel.common.core.publisher.SnsEventPublisher.class)
        );

        forecastService = new HeatForecastService(
                weatherClient,
                pointRepository,
                runRepository,
                dayRepository,
                normalSeeder,
                feedStateRepository,
                signalPublisher
        );
    }

    @Test
    @DisplayName("Calculates daily departures from climatological normals and publishes HeatSignal")
    void calculatesDeparturesAndPublishesSignal() {
        String jsonResponse = """
                {
                  "latitude": 28.66,
                  "longitude": 77.24,
                  "daily": {
                    "time": ["2026-05-15", "2026-05-16"],
                    "temperature_2m_max": [45.2, 46.5],
                    "apparent_temperature_max": [47.0, 48.2],
                    "relative_humidity_2m_max": [35.0, 30.0]
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("latitude=28.66")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<HeatForecastService.HeatSyncResult> resultOpt = forecastService.syncLive("yamuna-delhi");

        assertThat(resultOpt).isPresent();
        HeatForecastService.HeatSyncResult result = resultOpt.get();
        assertThat(result.locationId()).isEqualTo("yamuna-delhi");
        assertThat(result.daysCount()).isEqualTo(2);

        // Verify entities were stored in repositories
        List<HeatForecastDayEntity> savedDays = dayRepository.findByLocationId("yamuna-delhi");
        assertThat(savedDays).hasSize(2);

        HeatForecastDayEntity day1 = savedDays.get(0);
        assertThat(day1.getDate()).isEqualTo("2026-05-15");
        assertThat(day1.getTmax()).isEqualTo(45.2);
        // Departure should be tmax - normalTmax
        double expectedDeparture = Math.round((45.2 - day1.getNormalTmax()) * 10.0) / 10.0;
        assertThat(day1.getDeparture()).isEqualTo(expectedDeparture);

        // Verify HeatSignalPayload was published locally
        assertThat(capturedEvents).hasSize(1);
        Object event = capturedEvents.get(0);
        assertThat(event).isInstanceOf(EventEnvelope.class);
        @SuppressWarnings("unchecked")
        EventEnvelope<HeatSignalPayload> envelope = (EventEnvelope<HeatSignalPayload>) event;
        HeatSignalPayload signal = envelope.payload();

        assertThat(signal.locationId()).isEqualTo("yamuna-delhi");
        assertThat(signal.regionType()).isEqualTo(RegionType.PLAINS);
        assertThat(signal.days()).hasSize(2);
        assertThat(signal.days().get(0).tmax()).isEqualTo(45.2);
        assertThat(signal.days().get(0).departure()).isEqualTo(expectedDeparture);
    }
}
