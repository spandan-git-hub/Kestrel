package com.kestrel.common.events;

public record LocationUpsertedPayload(
    String locationId,
    String name,
    String state,
    double latitude,
    double longitude,
    String riverName,
    String basin,
    RegionType regionType,
    double threshold2yr,
    double threshold5yr,
    double threshold20yr,
    Language primaryLanguage,
    boolean active
) {
    public LocationUpsertedPayload(
        String locationId,
        String name,
        String state,
        double latitude,
        double longitude,
        String riverName,
        String basin,
        double threshold2yr,
        double threshold5yr,
        double threshold20yr,
        Language primaryLanguage,
        boolean active
    ) {
        this(locationId, name, state, latitude, longitude, riverName, basin, IndianRiverCatalog.getRegionType(locationId), threshold2yr, threshold5yr, threshold20yr, primaryLanguage, active);
    }

    public LocationUpsertedPayload(
        String locationId,
        String name,
        String state,
        double latitude,
        double longitude,
        String riverName,
        double threshold2yr,
        double threshold5yr,
        double threshold20yr,
        Language primaryLanguage,
        boolean active
    ) {
        this(locationId, name, state, latitude, longitude, riverName, "India", IndianRiverCatalog.getRegionType(locationId), threshold2yr, threshold5yr, threshold20yr, primaryLanguage, active);
    }
}
