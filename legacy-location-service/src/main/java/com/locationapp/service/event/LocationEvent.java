package com.locationapp.service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Location domain events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationEvent {
    private EventType eventType;
    private Long locationId;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private Long userId;
    private String username;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED,
        VIEWED
    }
}
