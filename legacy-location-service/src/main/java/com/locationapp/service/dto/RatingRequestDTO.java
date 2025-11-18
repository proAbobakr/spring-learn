package com.locationapp.service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating ratings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingRequestDTO {

    @NotNull(message = "Location ID is required")
    private Long locationId;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "1.0", message = "Score must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Score must not exceed 5.0")
    private Double score;

    @Size(max = 1000, message = "Review cannot exceed 1000 characters")
    private String review;
}
