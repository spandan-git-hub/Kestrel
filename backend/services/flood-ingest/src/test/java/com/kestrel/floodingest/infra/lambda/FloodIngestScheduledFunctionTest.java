package com.kestrel.floodingest.infra.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventObjectMapperFactory;
import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.SimulationScenario;
import com.kestrel.floodingest.api.dto.SyncLiveResponse;
import com.kestrel.floodingest.app.FloodForecastService;
import com.kestrel.floodingest.infra.budget.InMemoryCallBudgetTracker;
import com.kestrel.floodingest.infra.client.OpenMeteoClient;
import com.kestrel.floodingest.infra.entity.FloodSnapshotEntity;
import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import com.kestrel.floodingest.infra.publisher.FloodSignalEventPublisher;
import com.kestrel.floodingest.infra.repository.inmemory.InMemoryFloodSnapshotRepository;
import com.kestrel.floodingest.infra.repository.inmemory.InMemoryMonitoredPointReplicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class FloodIngestScheduledFunctionTest {

    private InMemoryMonitoredPointReplicaRepository replicaRepository;
    private InMemoryFloodSnapshotRepository snapshotRepository;
    private FloodForecastService forecastService;
    private FloodIngestScheduledFunction scheduledFunctionConfig;

    @BeforeEach
    void setUp() {
        replicaRepository = new InMemoryMonitoredPointReplicaRepository();
        snapshotRepository = new InMemoryFloodSnapshotRepository();
        ObjectMapper objectMapper = EventObjectMapperFactory.createObjectMapper();

        OpenMeteoClient mockClient = new OpenMeteoClient(
                RestClient.builder().baseUrl("https://flood-api.open-meteo.com/v1/flood").build(),
                objectMapper,
                new InMemoryCallBudgetTracker(500, 8000)
        );

        AtomicInteger publishedEvents = new AtomicInteger();
        ApplicationEventPublisher localPublisher = event -> publishedEvents.incrementAndGet();
        RestClient dummyEventBusClient = RestClient.builder().baseUrl("http://localhost:8083").build();
        FloodSignalEventPublisher eventPublisher = new FloodSignalEventPublisher(
                localPublisher,
                dummyEventBusClient,
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(com.kestrel.common.core.publisher.SnsEventPublisher.class)
        );

        forecastService = new FloodForecastService(
                replicaRepository,
                snapshotRepository,
                mockClient,
                eventPublisher,
                objectMapper
        );

        scheduledFunctionConfig = new FloodIngestScheduledFunction();
    }

    @Test
    @DisplayName("Seeds all 45 real Indian river stations across major basins")
    void shouldSeedAll45Stations() {
        forecastService.seedInitialLocationsIfEmpty();

        assertThat(replicaRepository.count()).isEqualTo(45);
        List<MonitoredPointReplica> all = replicaRepository.findAll();
        assertThat(all).hasSize(45);

        // Check key river stations from different basins
        assertThat(replicaRepository.findById("mumbai-kurla")).isPresent();
        assertThat(replicaRepository.findById("patna-ward12")).isPresent();
        assertThat(replicaRepository.findById("guwahati-river")).isPresent();
        assertThat(replicaRepository.findById("ganga-varanasi")).isPresent();
        assertThat(replicaRepository.findById("yamuna-delhi")).isPresent();
        assertThat(replicaRepository.findById("godavari-nashik")).isPresent();
        assertThat(replicaRepository.findById("krishna-sangli")).isPresent();
        assertThat(replicaRepository.findById("narmada-hoshangabad")).isPresent();
        assertThat(replicaRepository.findById("cauvery-srirangapatna")).isPresent();
        assertThat(replicaRepository.findById("periyar-aluva")).isPresent();
        assertThat(replicaRepository.findById("pamba-chengangur")).isPresent();
    }

    @Test
    @DisplayName("EventBridge scheduled function triggers daily flood sync for all active stations")
    void shouldTriggerScheduledSync() {
        forecastService.seedInitialLocationsIfEmpty();

        Function<Map<String, Object>, SyncLiveResponse> function = scheduledFunctionConfig.dailyFloodIngest(forecastService);

        Map<String, Object> eventBridgeEvent = Map.of(
                "version", "0",
                "id", "cdc73f9d-aea9-11e3-9d5a-835b769c0d9c",
                "detail-type", "Scheduled Event",
                "source", "aws.events",
                "time", "2026-10-29T06:30:00Z"
        );

        SyncLiveResponse response = function.apply(eventBridgeEvent);

        assertThat(response).isNotNull();
        assertThat(response.syncedLocations()).isEqualTo(45);
        assertThat(response.message()).contains("completed successfully");
    }

    @Test
    @DisplayName("Flood snapshot entity stores 10-day forecast with 30-day DynamoDB TTL")
    void shouldStoreSnapshotWith30DayTtl() {
        Instant now = Instant.now();
        FloodSnapshotEntity snapshot = new FloodSnapshotEntity(
                "ganga-varanasi",
                now,
                false,
                "LIVE_OPEN_METEO",
                51,
                "[]",
                false
        );

        long expectedTtl = now.getEpochSecond() + (30L * 24 * 3600);
        assertThat(snapshot.getTtl()).isEqualTo(expectedTtl);
        assertThat(snapshot.getLocationId()).isEqualTo("ganga-varanasi");
        assertThat(snapshot.getMemberCount()).isEqualTo(51);
    }
}
