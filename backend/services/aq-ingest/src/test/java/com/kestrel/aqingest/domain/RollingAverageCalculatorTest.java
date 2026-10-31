package com.kestrel.aqingest.domain;

import com.kestrel.common.events.AqSignalPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RollingAverageCalculatorTest {

    @Test
    @DisplayName("24-hour rolling mean requires at least 16 valid hours")
    void test24HourRollingMeanThreshold() {
        List<RollingAverageCalculator.RawHourlyAq> rawList = new ArrayList<>();

        // Create 24 hours of data where only 15 hours have PM2.5 values
        for (int h = 0; h < 24; h++) {
            String time = String.format("2026-10-31T%02d:00", h);
            Double pm25 = (h < 15) ? 50.0 : null; // 15 valid hours
            rawList.add(new RollingAverageCalculator.RawHourlyAq(time, 100.0, pm25, 30.0, 10.0, 40.0, 1000.0));
        }

        List<RollingAverageCalculator.ProcessedHourlyAq> processed =
                RollingAverageCalculator.processHourlyData(rawList);

        // At hour 23, valid hours is 15 -> rolling mean must be null (less than 16)
        assertThat(processed.get(23).pm25_24h()).isNull();
        assertThat(processed.get(23).validHours24()).isEqualTo(15);

        // Now update hour 23 to have a valid value, making it 16 valid hours
        rawList.set(23, new RollingAverageCalculator.RawHourlyAq(
                "2026-10-31T23:00", 100.0, 50.0, 30.0, 10.0, 40.0, 1000.0
        ));

        List<RollingAverageCalculator.ProcessedHourlyAq> processed16 =
                RollingAverageCalculator.processHourlyData(rawList);

        // At hour 23, valid hours is 16 -> rolling mean must be valid (50.0)
        assertThat(processed16.get(23).pm25_24h()).isNotNull();
        assertThat(processed16.get(23).pm25_24h()).isEqualTo(50.0);
        assertThat(processed16.get(23).validHours24()).isEqualTo(16);
    }

    @Test
    @DisplayName("8-hour rolling mean requires at least 6 valid hours")
    void test8HourRollingMeanThreshold() {
        List<RollingAverageCalculator.RawHourlyAq> rawList = new ArrayList<>();

        // Create 8 hours of data where only 5 hours have Ozone values
        for (int h = 0; h < 8; h++) {
            String time = String.format("2026-10-31T%02d:00", h);
            Double o3 = (h < 5) ? 40.0 : null; // 5 valid hours
            rawList.add(new RollingAverageCalculator.RawHourlyAq(time, 100.0, 50.0, 30.0, 10.0, o3, 1000.0));
        }

        List<RollingAverageCalculator.ProcessedHourlyAq> processed =
                RollingAverageCalculator.processHourlyData(rawList);

        // At hour 7, valid hours is 5 -> rolling mean must be null (less than 6)
        assertThat(processed.get(7).o3_8h()).isNull();

        // Update hour 7 to have a valid value, making it 6 valid hours
        rawList.set(7, new RollingAverageCalculator.RawHourlyAq(
                "2026-10-31T07:00", 100.0, 50.0, 30.0, 10.0, 40.0, 1000.0
        ));

        List<RollingAverageCalculator.ProcessedHourlyAq> processed6 =
                RollingAverageCalculator.processHourlyData(rawList);

        // At hour 7, valid hours is 6 -> rolling mean must be valid (40.0)
        assertThat(processed6.get(7).o3_8h()).isNotNull();
        assertThat(processed6.get(7).o3_8h()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("CPCB Carbon Monoxide unit conversion converts µg/m³ to mg/m³")
    void testCoUnitConversion() {
        List<RollingAverageCalculator.RawHourlyAq> rawList = new ArrayList<>();

        // 8 hours with CO = 1250 µg/m³
        for (int h = 0; h < 8; h++) {
            String time = String.format("2026-10-31T%02d:00", h);
            rawList.add(new RollingAverageCalculator.RawHourlyAq(time, 100.0, 50.0, 30.0, 10.0, 40.0, 1250.0));
        }

        List<RollingAverageCalculator.ProcessedHourlyAq> processed =
                RollingAverageCalculator.processHourlyData(rawList);

        // 1250 µg/m³ / 1000.0 = 1.25 mg/m³
        assertThat(processed.get(7).coMgm3()).isEqualTo(1.25);
        assertThat(processed.get(7).co_8h_mgm3()).isEqualTo(1.25);
    }

    @Test
    @DisplayName("extractCurrentPollutants extracts latest hour <= now")
    void testExtractCurrentPollutants() {
        List<RollingAverageCalculator.RawHourlyAq> rawList = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            String time = String.format("2026-10-31T%02d:00", h);
            rawList.add(new RollingAverageCalculator.RawHourlyAq(time, 100.0, 50.0 + h, 30.0, 10.0, 40.0, 1000.0));
        }

        List<RollingAverageCalculator.ProcessedHourlyAq> processed =
                RollingAverageCalculator.processHourlyData(rawList);

        LocalDateTime now = LocalDateTime.parse("2026-10-31T15:30:00");
        AqSignalPayload.CurrentPollutants current =
                RollingAverageCalculator.extractCurrentPollutants(processed, now);

        // Target hour should be 15:00
        assertThat(current.validHours()).isEqualTo(16); // hours 0 to 15 is 16 hours
        assertThat(current.pm25_24h()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("extractForecastPeaks computes peak rolling values grouped by forecast day")
    void testExtractForecastPeaks() {
        List<RollingAverageCalculator.RawHourlyAq> rawList = new ArrayList<>();
        // 48 hours: 24 on 2026-10-31 and 24 on 2026-11-01
        for (int h = 0; h < 24; h++) {
            rawList.add(new RollingAverageCalculator.RawHourlyAq(
                    String.format("2026-10-31T%02d:00", h), 100.0, 50.0, 30.0, 10.0, 40.0, 1000.0
            ));
        }
        for (int h = 0; h < 24; h++) {
            rawList.add(new RollingAverageCalculator.RawHourlyAq(
                    String.format("2026-11-01T%02d:00", h), 200.0, 120.0 + h, 60.0, 20.0, 80.0, 2000.0
            ));
        }

        List<RollingAverageCalculator.ProcessedHourlyAq> processed =
                RollingAverageCalculator.processHourlyData(rawList);

        List<AqSignalPayload.ForecastDayPeak> peaks =
                RollingAverageCalculator.extractForecastPeaks(processed, LocalDate.parse("2026-11-01"));

        assertThat(peaks).hasSize(1);
        AqSignalPayload.ForecastDayPeak peak = peaks.get(0);
        assertThat(peak.date()).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(peak.pm25_24h()).isGreaterThan(100.0);
        assertThat(peak.co_8h_mgm3()).isEqualTo(2.0); // 2000 / 1000
    }
}

