package com.locationapp.service.service;

import com.locationapp.service.dto.RatingRequestDTO;
import com.locationapp.service.dto.RatingResponseDTO;
import com.locationapp.service.event.RatingEvent;
import com.locationapp.service.exception.BadRequestException;
import com.locationapp.service.exception.ResourceNotFoundException;
import com.locationapp.service.model.Location;
import com.locationapp.service.model.Rating;
import com.locationapp.service.model.User;
import com.locationapp.service.repository.LocationRepository;
import com.locationapp.service.repository.RatingRepository;
import com.locationapp.service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rating Service
 */
@Service
@Slf4j
public class RatingService {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Add or update rating
     */
    @Transactional
    @CacheEvict(value = "locations", key = "#request.locationId")
    public RatingResponseDTO addOrUpdateRating(RatingRequestDTO request) {
        User user = getCurrentUser();
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", request.getLocationId()));

        // Check if user already rated this location
        Rating rating = ratingRepository.findByUserIdAndLocationId(user.getId(), location.getId())
                .orElse(null);

        RatingEvent.EventType eventType;
        if (rating == null) {
            // Create new rating
            rating = Rating.builder()
                    .score(request.getScore())
                    .review(request.getReview())
                    .user(user)
                    .location(location)
                    .build();
            eventType = RatingEvent.EventType.CREATED;
            log.info("New rating created for location: {} by user: {}", location.getId(), user.getUsername());
        } else {
            // Update existing rating
            rating.setScore(request.getScore());
            rating.setReview(request.getReview());
            eventType = RatingEvent.EventType.UPDATED;
            log.info("Rating updated for location: {} by user: {}", location.getId(), user.getUsername());
        }

        rating = ratingRepository.save(rating);

        // Update location's average rating
        location.recalculateAverageRating();
        locationRepository.save(location);

        // Publish event
        RatingEvent event = RatingEvent.builder()
                .eventType(eventType)
                .ratingId(rating.getId())
                .locationId(location.getId())
                .locationName(location.getName())
                .userId(user.getId())
                .username(user.getUsername())
                .score(rating.getScore())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishRatingEvent(event);

        return mapToResponse(rating);
    }

    /**
     * Get rating by ID
     */
    public RatingResponseDTO getRatingById(Long id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", "id", id));
        return mapToResponse(rating);
    }

    /**
     * Get all ratings for a location
     */
    public List<RatingResponseDTO> getRatingsForLocation(Long locationId) {
        List<Rating> ratings = ratingRepository.findByLocationIdOrderByCreatedAtDesc(locationId);
        return ratings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user's ratings
     */
    public List<RatingResponseDTO> getUserRatings(Long userId) {
        List<Rating> ratings = ratingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ratings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete rating
     */
    @Transactional
    @CacheEvict(value = "locations", key = "#id")
    public void deleteRating(Long id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", "id", id));

        User currentUser = getCurrentUser();
        if (!rating.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only delete your own ratings");
        }

        Location location = rating.getLocation();
        ratingRepository.delete(rating);

        // Update location's average rating
        location.recalculateAverageRating();
        locationRepository.save(location);

        // Publish event
        RatingEvent event = RatingEvent.builder()
                .eventType(RatingEvent.EventType.DELETED)
                .ratingId(id)
                .locationId(location.getId())
                .locationName(location.getName())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishRatingEvent(event);

        log.info("Rating deleted: {} by user: {}", id, currentUser.getUsername());
    }

    /**
     * Get rating distribution for a location
     */
    public List<Object[]> getRatingDistribution(Long locationId) {
        return ratingRepository.getRatingDistribution(locationId);
    }

    private User getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private RatingResponseDTO mapToResponse(Rating rating) {
        RatingResponseDTO dto = modelMapper.map(rating, RatingResponseDTO.class);
        dto.setLocationId(rating.getLocation().getId());
        dto.setLocationName(rating.getLocation().getName());
        dto.setUserId(rating.getUser().getId());
        dto.setUsername(rating.getUser().getUsername());
        return dto;
    }
}
