package com.locationapp.service.dto;

import com.locationapp.service.model.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for location responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private Location.Category category;
    private Double averageRating;
    private Integer totalRatings;
    private Long viewCount;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User info
    private Long userId;
    private String username;

    // Images
    private List<ImageResponseDTO> images;

    // Distance from search point (optional, calculated at runtime)
    private Double distanceKm;
}
