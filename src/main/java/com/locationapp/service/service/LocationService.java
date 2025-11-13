package com.locationapp.service.service;

import com.locationapp.service.dto.LocationRequestDTO;
import com.locationapp.service.dto.LocationResponseDTO;
import com.locationapp.service.event.LocationEvent;
import com.locationapp.service.exception.BadRequestException;
import com.locationapp.service.exception.ResourceNotFoundException;
import com.locationapp.service.model.Location;
import com.locationapp.service.model.User;
import com.locationapp.service.repository.LocationRepository;
import com.locationapp.service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Location Service
 *
 * Handles all location-related business logic with caching and events.
 * Similar to Repository pattern in Android with ViewModel.
 */
@Service
@Slf4j
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Create a new location
     */
    @Transactional
    public LocationResponseDTO createLocation(LocationRequestDTO request) {
        User user = getCurrentUser();

        Location location = Location.builder()
                .name(request.getName())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .category(request.getCategory())
                .user(user)
                .active(true)
                .viewCount(0L)
                .build();

        location = locationRepository.save(location);

        // Publish event
        LocationEvent event = LocationEvent.builder()
                .eventType(LocationEvent.EventType.CREATED)
                .locationId(location.getId())
                .locationName(location.getName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .userId(user.getId())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishLocationEvent(event);

        log.info("Location created: {} by user: {}", location.getId(), user.getUsername());

        return mapToResponse(location);
    }

    /**
     * Get location by ID (with caching)
     */
    @Cacheable(value = "locations", key = "#id")
    public LocationResponseDTO getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        // Increment view count asynchronously
        incrementViewCount(id);

        return mapToResponse(location);
    }

    /**
     * Update location
     */
    @Transactional
    @CachePut(value = "locations", key = "#id")
    public LocationResponseDTO updateLocation(Long id, LocationRequestDTO request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        User currentUser = getCurrentUser();
        if (!location.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only update your own locations");
        }

        // Update fields
        location.setName(request.getName());
        location.setDescription(request.getDescription());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setCountry(request.getCountry());
        location.setPostalCode(request.getPostalCode());
        location.setCategory(request.getCategory());

        location = locationRepository.save(location);

        // Publish event
        LocationEvent event = LocationEvent.builder()
                .eventType(LocationEvent.EventType.UPDATED)
                .locationId(location.getId())
                .locationName(location.getName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishLocationEvent(event);

        return mapToResponse(location);
    }

    /**
     * Delete location
     */
    @Transactional
    @CacheEvict(value = "locations", key = "#id")
    public void deleteLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        User currentUser = getCurrentUser();
        if (!location.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only delete your own locations");
        }

        // Soft delete
        location.setActive(false);
        locationRepository.save(location);

        // Publish event
        LocationEvent event = LocationEvent.builder()
                .eventType(LocationEvent.EventType.DELETED)
                .locationId(location.getId())
                .locationName(location.getName())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishLocationEvent(event);

        log.info("Location deleted: {} by user: {}", id, currentUser.getUsername());
    }

    /**
     * Get all locations with pagination
     */
    public Page<LocationResponseDTO> getAllLocations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Location> locations = locationRepository.findByActiveTrue(pageable);
        return locations.map(this::mapToResponse);
    }

    /**
     * Search locations by name
     */
    public List<LocationResponseDTO> searchLocations(String keyword) {
        List<Location> locations = locationRepository.searchByName(keyword);
        return locations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get nearby locations (with caching)
     */
    @Cacheable(value = "nearby", key = "#latitude + ':' + #longitude + ':' + #radiusKm")
    public List<LocationResponseDTO> getNearbyLocations(Double latitude, Double longitude, Double radiusKm, int limit) {
        List<Location> locations = locationRepository.findNearbyLocations(latitude, longitude, radiusKm, limit);
        return locations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get top-rated locations
     */
    public List<LocationResponseDTO> getTopRatedLocations(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Location> locations = locationRepository.findTopRated(pageable);
        return locations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get trending locations (cached for 5 minutes)
     */
    @Cacheable(value = "trending")
    public List<LocationResponseDTO> getTrendingLocations(int limit) {
        List<Location> locations = locationRepository.findTrending(limit);
        return locations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get locations by city
     */
    public List<LocationResponseDTO> getLocationsByCity(String city) {
        List<Location> locations = locationRepository.findByCityAndActiveTrue(city);
        return locations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get locations by category
     */
    public List<LocationResponseDTO> getLocationsByCategory(Location.Category category) {
        List<Location> locations = locationRepository.findByCategoryAndActiveTrue(category);
        return locations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user's locations
     */
    public List<LocationResponseDTO> getUserLocations(Long userId) {
        List<Location> locations = locationRepository.findByUserId(userId);
        return locations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Increment view count (async with Redis)
     */
    private void incrementViewCount(Long locationId) {
        try {
            String key = "location:views:" + locationId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);

            // Periodically update database (could be done with scheduled task)
            Object count = redisTemplate.opsForValue().get(key);
            if (count != null && Integer.parseInt(count.toString()) % 10 == 0) {
                locationRepository.findById(locationId).ifPresent(location -> {
                    location.incrementViewCount();
                    locationRepository.save(location);
                });
            }
        } catch (Exception e) {
            log.error("Error incrementing view count: {}", e.getMessage());
        }
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Map Location entity to DTO
     */
    private LocationResponseDTO mapToResponse(Location location) {
        LocationResponseDTO dto = modelMapper.map(location, LocationResponseDTO.class);
        dto.setUserId(location.getUser().getId());
        dto.setUsername(location.getUser().getUsername());
        return dto;
    }
}
