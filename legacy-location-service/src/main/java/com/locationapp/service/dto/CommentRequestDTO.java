package com.locationapp.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating comments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequestDTO {

    @NotNull(message = "Location ID is required")
    private Long locationId;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    private String content;

    // For replies: ID of parent comment (null for top-level comments)
    private Long parentCommentId;
}
