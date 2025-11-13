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
import java.util.ArrayList;
import java.util.List;

/**
 * Location Entity
 *
 * Represents a location/place that can be rated and reviewed by users.
 * Includes geolocation data (latitude/longitude) for map integration.
 */
@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_location_name", columnList = "name"),
    @Index(name = "idx_location_coords", columnList = "latitude, longitude"),
    @Index(name = "idx_location_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Category category = Category.OTHER;

    // Average rating (denormalized for performance)
    @Column(nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    // Total number of ratings
    @Column(nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;

    // Total views counter
    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

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

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Image> images = new ArrayList<>();

    // Helper methods
    public void addRating(Rating rating) {
        ratings.add(rating);
        rating.setLocation(this);
        recalculateAverageRating();
    }

    public void removeRating(Rating rating) {
        ratings.remove(rating);
        rating.setLocation(null);
        recalculateAverageRating();
    }

    private void recalculateAverageRating() {
        if (ratings.isEmpty()) {
            this.averageRating = 0.0;
            this.totalRatings = 0;
        } else {
            this.averageRating = ratings.stream()
                    .mapToDouble(Rating::getScore)
                    .average()
                    .orElse(0.0);
            this.totalRatings = ratings.size();
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public enum Category {
        RESTAURANT,
        CAFE,
        HOTEL,
        ATTRACTION,
        PARK,
        MUSEUM,
        SHOPPING,
        ENTERTAINMENT,
        EDUCATION,
        HEALTHCARE,
        TRANSPORTATION,
        SPORTS,
        OTHER
    }
}
