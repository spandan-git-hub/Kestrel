package com.kestrel.subscriberregistry.api.dto;

import com.kestrel.common.events.IndianRiverCatalog;
import com.kestrel.common.events.Language;
import com.kestrel.common.events.RegionType;
import com.kestrel.subscriberregistry.infra.entity.LocationEntity;

public record LocationResponse(
        String id,
        String name,
        String state,
        double latitude,
        double longitude,
        String riverName,
        String basin,
        String riverBasin,
        RegionType regionType,
        Language primaryLanguage,
        ThresholdsDto thresholds,
        double threshold2yr,
        double threshold5yr,
        double threshold20yr,
        boolean active
) {
    public record ThresholdsDto(
            double twoYear,
            double fiveYear,
            double twentyYear
    ) {}

    public static LocationResponse fromEntity(LocationEntity entity) {
        String basinVal = entity.getBasin() != null && !entity.getBasin().isBlank() ? entity.getBasin() : entity.getRiverName();
        RegionType regType = entity.getRegionType() != null ? entity.getRegionType() : IndianRiverCatalog.getRegionType(entity.getId());
        return new LocationResponse(
                entity.getId(),
                entity.getName(),
                entity.getState(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getRiverName(),
                basinVal,
                basinVal,
                regType,
                entity.getPrimaryLanguage(),
                new ThresholdsDto(
                        entity.getThreshold2yr(),
                        entity.getThreshold5yr(),
                        entity.getThreshold20yr()
                ),
                entity.getThreshold2yr(),
                entity.getThreshold5yr(),
                entity.getThreshold20yr(),
                entity.isActive()
        );
    }
}
