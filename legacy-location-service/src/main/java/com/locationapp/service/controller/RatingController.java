package com.locationapp.service.controller;

import com.locationapp.service.dto.RatingRequestDTO;
import com.locationapp.service.dto.RatingResponseDTO;
import com.locationapp.service.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rating Controller
 *
 * REST endpoints for location ratings.
 */
@RestController
@RequestMapping("/api/ratings")
@Tag(name = "Ratings", description = "Location rating endpoints")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Add or update rating", description = "Creates new or updates existing rating for a location")
    public ResponseEntity<RatingResponseDTO> addRating(@Valid @RequestBody RatingRequestDTO request) {
        RatingResponseDTO response = ratingService.addOrUpdateRating(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rating by ID", description = "Returns rating details")
    public ResponseEntity<RatingResponseDTO> getRating(@PathVariable Long id) {
        RatingResponseDTO response = ratingService.getRatingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/location/{locationId}")
    @Operation(summary = "Get ratings for location", description = "Returns all ratings for a specific location")
    public ResponseEntity<List<RatingResponseDTO>> getRatingsForLocation(@PathVariable Long locationId) {
        List<RatingResponseDTO> ratings = ratingService.getRatingsForLocation(locationId);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's ratings", description = "Returns all ratings created by a user")
    public ResponseEntity<List<RatingResponseDTO>> getUserRatings(@PathVariable Long userId) {
        List<RatingResponseDTO> ratings = ratingService.getUserRatings(userId);
        return ResponseEntity.ok(ratings);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete rating", description = "Deletes a rating (owner only)")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
        ratingService.deleteRating(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/location/{locationId}/distribution")
    @Operation(summary = "Get rating distribution", description = "Returns rating distribution (1-5 stars) for a location")
    public ResponseEntity<List<Object[]>> getRatingDistribution(@PathVariable Long locationId) {
        List<Object[]> distribution = ratingService.getRatingDistribution(locationId);
        return ResponseEntity.ok(distribution);
    }
}
