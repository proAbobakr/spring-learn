package com.locationapp.service.repository;

import com.locationapp.service.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Rating Repository
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    /**
     * Find all ratings for a location
     */
    List<Rating> findByLocationIdOrderByCreatedAtDesc(Long locationId);

    /**
     * Find all ratings by a user
     */
    List<Rating> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find a user's rating for a specific location
     */
    Optional<Rating> findByUserIdAndLocationId(Long userId, Long locationId);

    /**
     * Check if user has rated a location
     */
    boolean existsByUserIdAndLocationId(Long userId, Long locationId);

    /**
     * Get average rating for a location
     */
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.location.id = :locationId")
    Double getAverageRatingForLocation(@Param("locationId") Long locationId);

    /**
     * Get rating count for a location
     */
    long countByLocationId(Long locationId);

    /**
     * Get rating distribution for a location
     */
    @Query("SELECT r.score, COUNT(r) FROM Rating r WHERE r.location.id = :locationId GROUP BY r.score ORDER BY r.score DESC")
    List<Object[]> getRatingDistribution(@Param("locationId") Long locationId);

    /**
     * Find recent ratings (last N days)
     */
    @Query(value = """
        SELECT r.* FROM ratings r
        WHERE r.created_at > NOW() - INTERVAL ':days days'
        ORDER BY r.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Rating> findRecentRatings(@Param("days") int days, @Param("limit") int limit);
}
