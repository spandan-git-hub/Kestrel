package com.kestrel.floodingest.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kestrel.common.events.EventObjectMapperFactory;
import com.kestrel.floodingest.app.GumbelThresholdFittingService.ThresholdFitResult;
import com.kestrel.floodingest.infra.budget.InMemoryCallBudgetTracker;
import com.kestrel.floodingest.infra.client.OpenMeteoClient;
import com.kestrel.floodingest.infra.client.OpenMeteoClient.HistoricalDischargeResult;
import com.kestrel.floodingest.infra.entity.FloodThresholdEntity;
import com.kestrel.floodingest.infra.entity.MonitoredPointReplica;
import com.kestrel.floodingest.infra.repository.inmemory.InMemoryFloodThresholdRepository;
import com.kestrel.floodingest.infra.repository.inmemory.InMemoryMonitoredPointReplicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class GumbelThresholdFittingServiceTest {

    private TestOpenMeteoClient testOpenMeteoClient;
    private InMemoryMonitoredPointReplicaRepository replicaRepository;
    private InMemoryFloodThresholdRepository thresholdRepository;
    private GumbelThresholdFittingService fittingService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = EventObjectMapperFactory.createObjectMapper();
        testOpenMeteoClient = new TestOpenMeteoClient(objectMapper);
        replicaRepository = new InMemoryMonitoredPointReplicaRepository();
        thresholdRepository = new InMemoryFloodThresholdRepository();

        // Seed Yamuna Delhi
        MonitoredPointReplica delhi = new MonitoredPointReplica(
                "yamuna-delhi",
                "Old Railway Bridge, Delhi",
                "Delhi",
                28.6600,
                77.2400,
                "Yamuna River",
                "Ganga Basin",
                3200.0,
                5100.0,
                7800.0,
                com.kestrel.common.events.Language.hi,
                true
        );
        replicaRepository.save(delhi);

        fittingService = new GumbelThresholdFittingService(
                testOpenMeteoClient,
                replicaRepository,
                thresholdRepository
        );
    }

    @Test
    @DisplayName("Fits Gumbel extreme-value distribution from historical annual maxima and persists to DynamoDB")
    void shouldFitThresholdsAndPersist() {
        Map<Integer, Double> annualMaxima = new TreeMap<>();
        annualMaxima.put(2014, 2800.0);
        annualMaxima.put(2015, 3400.0);
        annualMaxima.put(2016, 4200.0);
        annualMaxima.put(2017, 3900.0);
        annualMaxima.put(2018, 5100.0);
        annualMaxima.put(2019, 6200.0);
        annualMaxima.put(2020, 3100.0);
        annualMaxima.put(2021, 4600.0);
        annualMaxima.put(2022, 5300.0);
        annualMaxima.put(2023, 7200.0);

        HistoricalDischargeResult histResult = new HistoricalDischargeResult(
                28.6600, 77.2400,
                LocalDate.of(2014, 1, 1), LocalDate.of(2023, 12, 31),
                List.of(), List.of(), annualMaxima
        );

        testOpenMeteoClient.setStubbedResult(Optional.of(histResult));

        ThresholdFitResult result = fittingService.fitThresholdsForLocation("yamuna-delhi");

        assertThat(result.success()).isTrue();
        assertThat(result.sampleCount()).isEqualTo(10);
        assertThat(result.mean()).isGreaterThan(4000.0);
        assertThat(result.stdDev()).isGreaterThan(0.0);
        assertThat(result.locationMu()).isGreaterThan(0.0);
        assertThat(result.scaleBeta()).isGreaterThan(0.0);

        // Monotonic threshold ordering: T20 > T5 > T2
        assertThat(result.threshold2yr()).isGreaterThan(0.0);
        assertThat(result.threshold5yr()).isGreaterThan(result.threshold2yr());
        assertThat(result.threshold20yr()).isGreaterThan(result.threshold5yr());

        // Verify updated in MonitoredPointReplicaRepository (kestrel_flood_monitored_point)
        MonitoredPointReplica updatedReplica = replicaRepository.findById("yamuna-delhi").orElseThrow();
        assertThat(updatedReplica.getThreshold2yr()).isEqualTo(result.threshold2yr());
        assertThat(updatedReplica.getThreshold5yr()).isEqualTo(result.threshold5yr());
        assertThat(updatedReplica.getThreshold20yr()).isEqualTo(result.threshold20yr());
        assertThat(updatedReplica.getGumbelMu()).isEqualTo(result.locationMu());
        assertThat(updatedReplica.getGumbelBeta()).isEqualTo(result.scaleBeta());
        assertThat(updatedReplica.getGumbelSampleYears()).isEqualTo(10);
        assertThat(updatedReplica.getThresholdsFittedAt()).isNotNull();

        // Verify persisted to FloodThresholdRepository (kestrel_flood_threshold)
        Optional<FloodThresholdEntity> persistedOpt = thresholdRepository.findById("yamuna-delhi");
        assertThat(persistedOpt).isPresent();
        FloodThresholdEntity entity = persistedOpt.get();
        assertThat(entity.getLocationId()).isEqualTo("yamuna-delhi");
        assertThat(entity.getRiverName()).isEqualTo("Yamuna River");
        assertThat(entity.getMean()).isEqualTo(result.mean());
        assertThat(entity.getThreshold20yr()).isEqualTo(result.threshold20yr());
        assertThat(entity.getSampleCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("Falls back to catalog baseline when historical data is unavailable")
    void shouldFallbackWhenHistoricalDataUnavailable() {
        testOpenMeteoClient.setStubbedResult(Optional.empty());

        ThresholdFitResult result = fittingService.fitThresholdsForLocation("yamuna-delhi");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Catalog baseline applied");
        assertThat(result.sampleCount()).isEqualTo(0);
        // Preserves existing catalog thresholds
        assertThat(result.threshold2yr()).isEqualTo(3200.0);
        assertThat(result.threshold5yr()).isEqualTo(5100.0);
        assertThat(result.threshold20yr()).isEqualTo(7800.0);
    }

    @Test
    @DisplayName("Falls back when historical data has fewer than 2 annual maxima")
    void shouldFallbackWhenInsufficientAnnualMaxima() {
        Map<Integer, Double> singleYear = Map.of(2023, 4500.0);
        HistoricalDischargeResult histResult = new HistoricalDischargeResult(
                28.6600, 77.2400,
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31),
                List.of(), List.of(), singleYear
        );

        testOpenMeteoClient.setStubbedResult(Optional.of(histResult));

        ThresholdFitResult result = fittingService.fitThresholdsForLocation("yamuna-delhi");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Insufficient historical observations");
    }

    static class TestOpenMeteoClient extends OpenMeteoClient {
        private Optional<HistoricalDischargeResult> stubbedResult = Optional.empty();

        public TestOpenMeteoClient(ObjectMapper mapper) {
            super(RestClient.builder().build(), mapper, new InMemoryCallBudgetTracker(500, 8000));
        }

        public void setStubbedResult(Optional<HistoricalDischargeResult> result) {
            this.stubbedResult = result;
        }

        @Override
        public Optional<HistoricalDischargeResult> fetchHistoricalDailyDischarge(double latitude, double longitude) {
            return stubbedResult;
        }

        @Override
        public Optional<HistoricalDischargeResult> fetchHistoricalDailyDischarge(
                double latitude, double longitude, LocalDate start, LocalDate end) {
            return stubbedResult;
        }
    }
}
