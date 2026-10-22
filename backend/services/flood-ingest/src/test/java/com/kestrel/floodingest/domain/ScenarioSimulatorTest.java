package com.kestrel.floodingest.domain;

import com.kestrel.common.events.FloodSignalPayload.ForecastDayStats;
import com.kestrel.common.events.SimulationScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScenarioSimulatorTest {

    private final LocalDate startDate = LocalDate.of(2026, 10, 22);
    private final double t2 = 45.0;
    private final double t5 = 85.0;
    private final double t20 = 140.0;

    @Test
    @DisplayName("CALM scenario produces 10 quiet days with 0 exceedance")
    void shouldGenerateCalmScenario() {
        List<ForecastDayStats> days = ScenarioSimulator.generateScenario(
                SimulationScenario.CALM, startDate, t2, t5, t20);

        assertThat(days).hasSize(10);
        for (ForecastDayStats day : days) {
            assertThat(day.share2yr()).isEqualTo(0.0);
            assertThat(day.share5yr()).isEqualTo(0.0);
            assertThat(day.share20yr()).isEqualTo(0.0);
            assertThat(day.qMax()).isLessThan(t2);
        }
    }

    @Test
    @DisplayName("WATCH_2YR scenario crests at Day 3 with S2=0.42, S5=0.05, S20=0.00")
    void shouldGenerateWatchScenario() {
        List<ForecastDayStats> days = ScenarioSimulator.generateScenario(
                SimulationScenario.WATCH_2YR, startDate, t2, t5, t20);

        assertThat(days).hasSize(10);
        ForecastDayStats crestDay = days.get(3);
        assertThat(crestDay.share2yr()).isEqualTo(0.42);
        assertThat(crestDay.share5yr()).isEqualTo(0.05);
        assertThat(crestDay.share20yr()).isEqualTo(0.00);
    }

    @Test
    @DisplayName("WARNING_5YR scenario crests at Day 2 with S2=0.95, S5=0.68, S20=0.15")
    void shouldGenerateWarningScenario() {
        List<ForecastDayStats> days = ScenarioSimulator.generateScenario(
                SimulationScenario.WARNING_5YR, startDate, t2, t5, t20);

        assertThat(days).hasSize(10);
        ForecastDayStats crestDay = days.get(2);
        assertThat(crestDay.share2yr()).isEqualTo(0.95);
        assertThat(crestDay.share5yr()).isEqualTo(0.68);
        assertThat(crestDay.share20yr()).isEqualTo(0.15);
    }

    @Test
    @DisplayName("SEVERE_20YR scenario crests at Day 1 and 2 with S2=1.00, S5=0.92, S20=0.78")
    void shouldGenerateSevereScenario() {
        List<ForecastDayStats> days = ScenarioSimulator.generateScenario(
                SimulationScenario.SEVERE_20YR, startDate, t2, t5, t20);

        assertThat(days).hasSize(10);
        ForecastDayStats day1 = days.get(1);
        ForecastDayStats day2 = days.get(2);

        assertThat(day1.share2yr()).isEqualTo(1.00);
        assertThat(day1.share5yr()).isEqualTo(0.92);
        assertThat(day1.share20yr()).isEqualTo(0.78);

        assertThat(day2.share2yr()).isEqualTo(1.00);
        assertThat(day2.share5yr()).isEqualTo(0.92);
        assertThat(day2.share20yr()).isEqualTo(0.78);
    }

    @Test
    @DisplayName("LIVE_OPEN_METEO scenario throws exception when passed to generator directly")
    void shouldRejectLiveOpenMeteoInGenerator() {
        assertThatThrownBy(() -> ScenarioSimulator.generateScenario(
                SimulationScenario.LIVE_OPEN_METEO, startDate, t2, t5, t20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LIVE_OPEN_METEO");
    }
}
