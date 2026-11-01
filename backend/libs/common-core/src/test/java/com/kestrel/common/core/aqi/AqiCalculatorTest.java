package com.kestrel.common.core.aqi;

import com.kestrel.common.events.AqiCategory;
import com.kestrel.common.events.Pollutant;
import com.kestrel.common.events.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AqiCalculator - Official CPCB AQI Calculation Tests")
class AqiCalculatorTest {

    @Test
    @DisplayName("Mandatory acceptance case 1: PM2.5 = 45 µg/m³ yields AQI 75 (Satisfactory)")
    void pm25SubIndexSatisfactory() {
        assertEquals(75, AqiCalculator.calculateSubIndex(Pollutant.PM25, 45.0));
    }

    @Test
    @DisplayName("Mandatory acceptance case 2: PM2.5 = 150 µg/m³ yields AQI 323 (Very Poor)")
    void pm25SubIndexVeryPoor() {
        assertEquals(323, AqiCalculator.calculateSubIndex(Pollutant.PM25, 150.0));
    }

    @Test
    @DisplayName("Mandatory acceptance case 3: Insufficient data (<3 pollutants) throws InsufficientDataException")
    void insufficientDataFewerThanThreePollutantsThrows() {
        assertThrows(InsufficientDataException.class, () ->
                AqiCalculator.calculateAqi(Map.of(Pollutant.SO2, 30.0, Pollutant.NO2, 40.0)));
    }

    @Test
    @DisplayName("CPCB Rule: >=3 pollutants without PM2.5 or PM10 throws InsufficientDataException")
    void insufficientDataMissingParticulateMatterThrows() {
        assertThrows(InsufficientDataException.class, () ->
                AqiCalculator.calculateAqi(Map.of(
                        Pollutant.SO2, 30.0,
                        Pollutant.NO2, 40.0,
                        Pollutant.O3, 50.0
                )));
    }

    @Test
    @DisplayName("Valid 3 pollutants with PM2.5 calculates overall max AQI correctly")
    void validThreePollutantsCalculatesMaxAqi() {
        // PM2.5 = 45 (sub-index 75), NO2 = 40 (sub-index 50), SO2 = 40 (sub-index 50)
        Map<Pollutant, Double> concentrations = Map.of(
                Pollutant.PM25, 45.0,
                Pollutant.NO2, 40.0,
                Pollutant.SO2, 40.0
        );
        int aqi = AqiCalculator.calculateAqi(concentrations);
        assertEquals(75, aqi);

        AqiCalculator.AqiResult detailed = AqiCalculator.calculateDetailedAqi(concentrations);
        assertEquals(75, detailed.aqi());
        assertEquals(AqiCategory.SATISFACTORY, detailed.category());
        assertEquals(Pollutant.PM25, detailed.dominantPollutant());
        assertEquals(75, detailed.subIndices().get(Pollutant.PM25));
        assertEquals(50, detailed.subIndices().get(Pollutant.NO2));
        assertEquals(50, detailed.subIndices().get(Pollutant.SO2));
    }

    @Test
    @DisplayName("Valid pollutants with PM10 as particulate indicator")
    void validThreePollutantsWithPm10() {
        Map<Pollutant, Double> concentrations = Map.of(
                Pollutant.PM10, 80.0,
                Pollutant.NO2, 30.0,
                Pollutant.CO, 0.5
        );
        AqiCalculator.AqiResult detailed = AqiCalculator.calculateDetailedAqi(concentrations);
        assertTrue(detailed.aqi() > 0);
        assertEquals(Pollutant.PM10, detailed.dominantPollutant());
    }

    @Test
    @DisplayName("CPCB Category to Severity mapping")
    void categoryToSeverityMapping() {
        assertEquals(Severity.NONE, AqiCalculator.toSeverity(AqiCategory.GOOD));
        assertEquals(Severity.NONE, AqiCalculator.toSeverity(AqiCategory.SATISFACTORY));
        assertEquals(Severity.NONE, AqiCalculator.toSeverity(AqiCategory.MODERATE));
        assertEquals(Severity.WATCH, AqiCalculator.toSeverity(AqiCategory.POOR));
        assertEquals(Severity.WARNING, AqiCalculator.toSeverity(AqiCategory.VERY_POOR));
        assertEquals(Severity.SEVERE, AqiCalculator.toSeverity(AqiCategory.SEVERE));

        // Directly by AQI value
        assertEquals(Severity.NONE, AqiCalculator.toSeverity(45));
        assertEquals(Severity.NONE, AqiCalculator.toSeverity(150));
        assertEquals(Severity.WATCH, AqiCalculator.toSeverity(250));
        assertEquals(Severity.WARNING, AqiCalculator.toSeverity(350));
        assertEquals(Severity.SEVERE, AqiCalculator.toSeverity(450));
    }

    @Test
    @DisplayName("Breakpoints boundary values for PM2.5, PM10, NO2, SO2, O3, CO")
    void breakpointBoundaries() {
        // PM2.5
        assertEquals(0, AqiCalculator.calculateSubIndex(Pollutant.PM25, 0.0));
        assertEquals(50, AqiCalculator.calculateSubIndex(Pollutant.PM25, 30.0));
        assertEquals(100, AqiCalculator.calculateSubIndex(Pollutant.PM25, 60.0));
        assertEquals(200, AqiCalculator.calculateSubIndex(Pollutant.PM25, 90.0));
        assertEquals(300, AqiCalculator.calculateSubIndex(Pollutant.PM25, 120.0));
        assertEquals(400, AqiCalculator.calculateSubIndex(Pollutant.PM25, 250.0));
        assertEquals(500, AqiCalculator.calculateSubIndex(Pollutant.PM25, 500.0)); // Capped at 500

        // PM10
        assertEquals(50, AqiCalculator.calculateSubIndex(Pollutant.PM10, 50.0));
        assertEquals(100, AqiCalculator.calculateSubIndex(Pollutant.PM10, 100.0));

        // NO2
        assertEquals(50, AqiCalculator.calculateSubIndex(Pollutant.NO2, 40.0));
        assertEquals(100, AqiCalculator.calculateSubIndex(Pollutant.NO2, 80.0));

        // SO2
        assertEquals(50, AqiCalculator.calculateSubIndex(Pollutant.SO2, 40.0));
        assertEquals(100, AqiCalculator.calculateSubIndex(Pollutant.SO2, 80.0));

        // O3
        assertEquals(50, AqiCalculator.calculateSubIndex(Pollutant.O3, 50.0));
        assertEquals(100, AqiCalculator.calculateSubIndex(Pollutant.O3, 100.0));

        // CO (mg/m³)
        assertEquals(50, AqiCalculator.calculateSubIndex(Pollutant.CO, 1.0));
        assertEquals(100, AqiCalculator.calculateSubIndex(Pollutant.CO, 2.0));
    }
}

