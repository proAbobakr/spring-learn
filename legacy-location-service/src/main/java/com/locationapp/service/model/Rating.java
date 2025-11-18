package com.locationapp.service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Rating Entity
 *
 * Represents a rating given by a user to a location.
 * Score is between 1 and 5 stars.
 */
@Entity
@Table(name = "ratings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_location_rating",
                         columnNames = {"user_id", "location_id"})
    },
    indexes = {
        @Index(name = "idx_rating_location", columnList = "location_id"),
        @Index(name = "idx_rating_user", columnList = "user_id"),
        @Index(name = "idx_rating_score", columnList = "score")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double score; // 1.0 to 5.0

    @Column(length = 1000)
    private String review;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    // Validation
    @PrePersist
    @PreUpdate
    private void validateScore() {
        if (score < 1.0 || score > 5.0) {
            throw new IllegalArgumentException("Rating score must be between 1.0 and 5.0");
        }
    }
}
