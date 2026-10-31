package com.kestrel.heatingest.domain;

import com.kestrel.common.events.RegionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WmoNormalsGeneratorTest {

    @Test
    @DisplayName("Generates complete 366-day normals for Plains location")
    void generatesCompleteNormalsForPlains() {
        Map<Integer, Double> normals = WmoNormalsGenerator.generate366DailyNormals("yamuna-delhi", RegionType.PLAINS, 28.66);

        assertThat(normals).hasSize(366);
        for (int doy = 1; doy <= 366; doy++) {
            assertThat(normals).containsKey(doy);
            Double val = normals.get(doy);
            assertThat(val).isNotNull();
            // Temperature in northern plains should be within [15.0, 45.0]
            assertThat(val).isBetween(15.0, 45.0);
        }

        // Mid-May/early June (DOY 140 - 155) should be summer peak in Delhi (> 39.0°C)
        double peakSummerNormal = normals.get(145);
        assertThat(peakSummerNormal).isGreaterThan(39.0);

        // January (DOY 10) should be winter minimum (< 24.0°C)
        double winterNormal = normals.get(10);
        assertThat(winterNormal).isLessThan(24.0);
    }

    @Test
    @DisplayName("Generates moderated normals for Coastal location")
    void generatesModeratedNormalsForCoastal() {
        Map<Integer, Double> normals = WmoNormalsGenerator.generate366DailyNormals("mumbai-kurla", RegionType.COASTAL, 19.07);

        assertThat(normals).hasSize(366);
        double maxTemp = normals.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minTemp = normals.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

        // Coastal climate has smaller annual range
        assertThat(maxTemp).isLessThanOrEqualTo(36.0);
        assertThat(minTemp).isGreaterThanOrEqualTo(28.0);
    }

    @Test
    @DisplayName("Generates cool normals for Hilly location")
    void generatesCoolNormalsForHilly() {
        Map<Integer, Double> normals = WmoNormalsGenerator.generate366DailyNormals("jhelum-srinagar", RegionType.HILLY, 34.08);

        assertThat(normals).hasSize(366);
        double winterMin = normals.get(15);
        double summerMax = normals.get(190);

        // Hilly regions have cold winter and moderate summer
        assertThat(winterMin).isLessThan(15.0);
        assertThat(summerMax).isLessThanOrEqualTo(32.0);
    }

    @Test
    @DisplayName("Smoothing filter ensures smooth transitions across circular year boundary")
    void smoothingFilterEnsuresContinuity() {
        Map<Integer, Double> normals = WmoNormalsGenerator.generate366DailyNormals("ganga-varanasi", RegionType.PLAINS, 25.31);

        double day366 = normals.get(366);
        double day1 = normals.get(1);

        // Difference between Dec 31 (day 366) and Jan 1 (day 1) should be very small (< 0.5°C)
        assertThat(Math.abs(day366 - day1)).isLessThan(0.5);
    }
}

