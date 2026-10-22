package com.kestrel.web;

import com.kestrel.repository.LocationRepository;
import com.kestrel.web.dto.LocationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
@CrossOrigin(origins = "*")
@Tag(name = "Locations", description = "Monitored pilot location registry")
public class LocationController {

    private final LocationRepository locationRepository;

    public LocationController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @GetMapping
    @Operation(summary = "Get all monitored pilot locations")
    public List<LocationDto> getLocations() {
        return locationRepository.findAll().stream()
            .map(LocationDto::fromEntity)
            .toList();
    }
}
