package com.locationapp.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for rating responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponseDTO {
    private Long id;
    private Long locationId;
    private String locationName;
    private Long userId;
    private String username;
    private Double score;
    private String review;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
