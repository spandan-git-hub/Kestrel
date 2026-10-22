package com.kestrel.common.core.geo;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {}

    /**
     * Calculates the great-circle distance between two geographic points in kilometers
     * using the Haversine formula.
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(rLat1) * Math.cos(rLat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Checks if coordinates fall within a rectangular bounding box.
     */
    public static boolean isWithinBoundingBox(
        double lat, double lon,
        double minLat, double maxLat,
        double minLon, double maxLon
    ) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
        public boolean contains(double lat, double lon) {
            return isWithinBoundingBox(lat, lon, minLat, maxLat, minLon, maxLon);
        }
    }
}
