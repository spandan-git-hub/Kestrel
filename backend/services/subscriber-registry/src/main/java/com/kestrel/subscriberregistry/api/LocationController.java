package com.kestrel.subscriberregistry.api;

import com.kestrel.subscriberregistry.api.dto.LocationResponse;
import com.kestrel.subscriberregistry.app.RegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/registry/locations")
public class LocationController {

    private final RegistryService registryService;

    public LocationController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAllLocations() {
        return ResponseEntity.ok(registryService.getAllLocations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocationById(@PathVariable String id) {
        return ResponseEntity.ok(registryService.getLocationById(id));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncLocations() {
        int count = registryService.syncLocations();
        return ResponseEntity.ok(Map.of(
                "status", "SYNCED",
                "syncedLocationsCount", count
        ));
    }
}
