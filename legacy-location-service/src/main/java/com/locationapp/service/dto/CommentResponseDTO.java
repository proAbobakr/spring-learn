package com.locationapp.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for comment responses (with nested replies)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDTO {
    private Long id;
    private Long locationId;
    private String locationName;
    private Long userId;
    private String username;
    private String userProfileImage;
    private String content;
    private Boolean edited;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Parent comment info (for replies)
    private Long parentCommentId;

    // Nested replies
    @Builder.Default
    private List<CommentResponseDTO> replies = new ArrayList<>();

    // Flags
    private Boolean isReply;
    private Integer replyCount;
}
