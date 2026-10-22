package com.kestrel.common.core.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void testHaversineDistanceBetweenMumbaiAndPune() {
        // Mumbai Kurla: 19.0726, 72.8797
        // Pune: 18.5204, 73.8567
        double distance = GeoUtils.haversineDistanceKm(19.0726, 72.8797, 18.5204, 73.8567);
        // Direct distance is approximately 119-125 km
        assertThat(distance).isCloseTo(120.0, within(10.0));
    }

    @Test
    void testHaversineDistanceSamePoint() {
        double distance = GeoUtils.haversineDistanceKm(19.0726, 72.8797, 19.0726, 72.8797);
        assertThat(distance).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testBoundingBox() {
        GeoUtils.BoundingBox box = new GeoUtils.BoundingBox(18.0, 20.0, 72.0, 74.0);
        assertThat(box.contains(19.0726, 72.8797)).isTrue();
        assertThat(box.contains(25.61, 85.14)).isFalse(); // Patna
    }
}
