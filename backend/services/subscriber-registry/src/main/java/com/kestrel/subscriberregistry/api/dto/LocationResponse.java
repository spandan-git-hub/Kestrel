package com.kestrel.subscriberregistry.api.dto;

import com.kestrel.common.events.Language;
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
        return new LocationResponse(
                entity.getId(),
                entity.getName(),
                entity.getState(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getRiverName(),
                basinVal,
                basinVal,
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
