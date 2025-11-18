package com.locationapp.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for image responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResponseDTO {
    private Long id;
    private Long locationId;
    private Long userId;
    private String username;
    private String fileName;
    private String url;
    private String contentType;
    private Long fileSize;
    private String caption;
    private Boolean isPrimary;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
