package com.kestrel.web.dto;

import com.kestrel.domain.Language;
import com.kestrel.domain.LocationEntity;

public record LocationDto(
    String id,
    String name,
    String state,
    Double latitude,
    Double longitude,
    String riverName,
    Language primaryLanguage,
    ThresholdsDto thresholds
) {
    public record ThresholdsDto(
        Double twoYear,
        Double fiveYear,
        Double twentyYear
    ) {}

    public static LocationDto fromEntity(LocationEntity entity) {
        return new LocationDto(
            entity.getId(),
            entity.getName(),
            entity.getState(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getRiverName(),
            entity.getPrimaryLanguage(),
            new ThresholdsDto(
                entity.getThreshold2yr(),
                entity.getThreshold5yr(),
                entity.getThreshold20yr()
            )
        );
    }
}
