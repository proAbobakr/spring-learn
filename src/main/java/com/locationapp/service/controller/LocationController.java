package com.locationapp.service.controller;

import com.locationapp.service.dto.LocationRequestDTO;
import com.locationapp.service.dto.LocationResponseDTO;
import com.locationapp.service.model.Location;
import com.locationapp.service.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Location Controller
 *
 * REST endpoints for location management with maps integration.
 */
@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Location management endpoints")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a new location", description = "Creates a new location with coordinates")
    public ResponseEntity<LocationResponseDTO> createLocation(@Valid @RequestBody LocationRequestDTO request) {
        LocationResponseDTO response = locationService.createLocation(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID", description = "Returns location details with ratings and images")
    public ResponseEntity<LocationResponseDTO> getLocation(@PathVariable Long id) {
        LocationResponseDTO response = locationService.getLocationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update location", description = "Updates an existing location (owner only)")
    public ResponseEntity<LocationResponseDTO> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequestDTO request
    ) {
        LocationResponseDTO response = locationService.updateLocation(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete location", description = "Soft deletes a location (owner only)")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all locations", description = "Returns paginated list of all active locations")
    public ResponseEntity<Page<LocationResponseDTO>> getAllLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<LocationResponseDTO> locations = locationService.getAllLocations(page, size);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/search")
    @Operation(summary = "Search locations", description = "Search locations by name keyword")
    public ResponseEntity<List<LocationResponseDTO>> searchLocations(@RequestParam String keyword) {
        List<LocationResponseDTO> locations = locationService.searchLocations(keyword);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby locations", description = "Find locations within specified radius (km)")
    public ResponseEntity<List<LocationResponseDTO>> getNearbyLocations(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<LocationResponseDTO> locations = locationService.getNearbyLocations(
                latitude, longitude, radiusKm, limit
        );
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get top-rated locations", description = "Returns highest rated locations")
    public ResponseEntity<List<LocationResponseDTO>> getTopRated(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<LocationResponseDTO> locations = locationService.getTopRatedLocations(limit);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending locations", description = "Returns most viewed locations recently")
    public ResponseEntity<List<LocationResponseDTO>> getTrending(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<LocationResponseDTO> locations = locationService.getTrendingLocations(limit);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/by-city")
    @Operation(summary = "Get locations by city", description = "Returns all locations in a specific city")
    public ResponseEntity<List<LocationResponseDTO>> getByCity(@RequestParam String city) {
        List<LocationResponseDTO> locations = locationService.getLocationsByCity(city);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/by-category")
    @Operation(summary = "Get locations by category", description = "Returns all locations of a specific category")
    public ResponseEntity<List<LocationResponseDTO>> getByCategory(@RequestParam Location.Category category) {
        List<LocationResponseDTO> locations = locationService.getLocationsByCategory(category);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's locations", description = "Returns all locations created by a user")
    public ResponseEntity<List<LocationResponseDTO>> getUserLocations(@PathVariable Long userId) {
        List<LocationResponseDTO> locations = locationService.getUserLocations(userId);
        return ResponseEntity.ok(locations);
    }
}
