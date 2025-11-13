package com.locationapp.service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Comment domain events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEvent {
    private EventType eventType;
    private Long commentId;
    private Long locationId;
    private String locationName;
    private Long userId;
    private String username;
    private String content;
    private Long parentCommentId;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED,
        LIKED
    }
}
