package com.locationapp.service.repository;

import com.locationapp.service.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Image Repository
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * Find all images for a location
     */
    List<Image> findByLocationIdOrderByDisplayOrderAsc(Long locationId);

    /**
     * Find primary image for a location
     */
    Optional<Image> findByLocationIdAndIsPrimaryTrue(Long locationId);

    /**
     * Find all images uploaded by a user
     */
    List<Image> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Count images for a location
     */
    long countByLocationId(Long locationId);

    /**
     * Find images by filename
     */
    Optional<Image> findByFileName(String fileName);
}
