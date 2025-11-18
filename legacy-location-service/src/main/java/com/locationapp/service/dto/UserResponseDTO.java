package com.locationapp.service.dto;

import com.locationapp.service.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user profile responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String profileImageUrl;
    private User.Role role;
    private LocalDateTime createdAt;

    // Statistics
    private Integer totalLocations;
    private Integer totalRatings;
    private Integer totalComments;
}
