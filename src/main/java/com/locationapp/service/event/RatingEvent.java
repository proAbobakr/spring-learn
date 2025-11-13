package com.locationapp.service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Rating domain events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingEvent {
    private EventType eventType;
    private Long ratingId;
    private Long locationId;
    private String locationName;
    private Long userId;
    private String username;
    private Double score;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}
