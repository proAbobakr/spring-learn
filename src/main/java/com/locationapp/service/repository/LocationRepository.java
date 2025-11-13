package com.locationapp.service.repository;

import com.locationapp.service.model.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Location Repository
 *
 * Includes geospatial queries for finding nearby locations
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    /**
     * Find locations by user
     */
    List<Location> findByUserId(Long userId);

    /**
     * Find active locations by category
     */
    List<Location> findByCategoryAndActiveTrue(Location.Category category);

    /**
     * Find locations with pagination
     */
    Page<Location> findByActiveTrue(Pageable pageable);

    /**
     * Search locations by name (case-insensitive)
     */
    @Query("SELECT l FROM Location l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND l.active = true")
    List<Location> searchByName(@Param("keyword") String keyword);

    /**
     * Find top-rated locations
     */
    @Query("SELECT l FROM Location l WHERE l.active = true AND l.totalRatings > 0 ORDER BY l.averageRating DESC, l.totalRatings DESC")
    Page<Location> findTopRated(Pageable pageable);

    /**
     * Find nearby locations using Haversine formula
     * This calculates distance between two points on Earth
     *
     * Formula: distance = 6371 * acos(cos(radians(lat1)) * cos(radians(lat2)) *
     *                     cos(radians(lng2) - radians(lng1)) +
     *                     sin(radians(lat1)) * sin(radians(lat2)))
     */
    @Query(value = """
        SELECT l.*,
               (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
                cos(radians(l.longitude) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(l.latitude)))) AS distance
        FROM locations l
        WHERE l.active = true
        HAVING distance < :radiusKm
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Location> findNearbyLocations(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radiusKm") Double radiusKm,
        @Param("limit") int limit
    );

    /**
     * Find locations within a bounding box (for map view)
     */
    @Query("SELECT l FROM Location l WHERE l.active = true AND " +
           "l.latitude BETWEEN :minLat AND :maxLat AND " +
           "l.longitude BETWEEN :minLng AND :maxLng")
    List<Location> findInBoundingBox(
        @Param("minLat") Double minLat,
        @Param("maxLat") Double maxLat,
        @Param("minLng") Double minLng,
        @Param("maxLng") Double maxLng
    );

    /**
     * Get trending locations (most viewed in last 30 days)
     */
    @Query(value = """
        SELECT l.* FROM locations l
        WHERE l.active = true
        AND l.created_at > NOW() - INTERVAL '30 days'
        ORDER BY l.view_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Location> findTrending(@Param("limit") int limit);

    /**
     * Find locations by city
     */
    List<Location> findByCityAndActiveTrue(String city);

    /**
     * Find locations by country
     */
    List<Location> findByCountryAndActiveTrue(String country);

    /**
     * Get location count by category
     */
    @Query("SELECT l.category, COUNT(l) FROM Location l WHERE l.active = true GROUP BY l.category")
    List<Object[]> countByCategory();
}
