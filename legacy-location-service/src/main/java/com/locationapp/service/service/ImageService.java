package com.locationapp.service.service;

import com.locationapp.service.dto.ImageResponseDTO;
import com.locationapp.service.exception.BadRequestException;
import com.locationapp.service.exception.ResourceNotFoundException;
import com.locationapp.service.model.Image;
import com.locationapp.service.model.Location;
import com.locationapp.service.model.User;
import com.locationapp.service.repository.ImageRepository;
import com.locationapp.service.repository.LocationRepository;
import com.locationapp.service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Image Service
 *
 * Handles image upload, storage, and retrieval.
 */
@Service
@Slf4j
public class ImageService {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.max-size:5242880}") // 5MB default
    private Long maxFileSize;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    /**
     * Upload image for a location
     */
    @Transactional
    public ImageResponseDTO uploadImage(
            Long locationId,
            MultipartFile file,
            String caption,
            Boolean isPrimary
    ) throws IOException {
        User user = getCurrentUser();
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));

        // Validate file
        validateFile(file);

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;

        // Save file to disk
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create image record
        Image image = Image.builder()
                .fileName(filename)
                .filePath(filePath.toString())
                .url("/api/images/view/" + filename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .caption(caption)
                .isPrimary(isPrimary != null ? isPrimary : false)
                .location(location)
                .user(user)
                .build();

        // If this is primary image, unset other primary images
        if (image.getIsPrimary()) {
            imageRepository.findByLocationIdAndIsPrimaryTrue(locationId)
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setIsPrimary(false);
                        imageRepository.save(existingPrimary);
                    });
        }

        image = imageRepository.save(image);

        log.info("Image uploaded: {} for location: {} by user: {}",
                filename, locationId, user.getUsername());

        return mapToResponse(image);
    }

    /**
     * Get image by ID
     */
    public ImageResponseDTO getImageById(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", id));
        return mapToResponse(image);
    }

    /**
     * Get all images for a location
     */
    public List<ImageResponseDTO> getImagesForLocation(Long locationId) {
        List<Image> images = imageRepository.findByLocationIdOrderByDisplayOrderAsc(locationId);
        return images.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get image file by filename
     */
    public Path getImageFile(String filename) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Image file not found: " + filename);
        }

        return filePath;
    }

    /**
     * Update image metadata
     */
    @Transactional
    public ImageResponseDTO updateImage(Long id, String caption, Boolean isPrimary) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", id));

        User currentUser = getCurrentUser();
        if (!image.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only update your own images");
        }

        if (caption != null) {
            image.setCaption(caption);
        }

        if (isPrimary != null && isPrimary) {
            // Unset other primary images for this location
            imageRepository.findByLocationIdAndIsPrimaryTrue(image.getLocation().getId())
                    .ifPresent(existingPrimary -> {
                        if (!existingPrimary.getId().equals(id)) {
                            existingPrimary.setIsPrimary(false);
                            imageRepository.save(existingPrimary);
                        }
                    });
            image.setIsPrimary(true);
        }

        image = imageRepository.save(image);
        return mapToResponse(image);
    }

    /**
     * Delete image
     */
    @Transactional
    public void deleteImage(Long id) throws IOException {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", id));

        User currentUser = getCurrentUser();
        if (!image.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only delete your own images");
        }

        // Delete file from disk
        Path filePath = Paths.get(image.getFilePath());
        Files.deleteIfExists(filePath);

        // Delete database record
        imageRepository.delete(image);

        log.info("Image deleted: {} by user: {}", id, currentUser.getUsername());
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(
                    String.format("File size exceeds maximum allowed size of %d bytes", maxFileSize)
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES)
            );
        }
    }

    private User getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ImageResponseDTO mapToResponse(Image image) {
        ImageResponseDTO dto = modelMapper.map(image, ImageResponseDTO.class);
        dto.setLocationId(image.getLocation().getId());
        dto.setUserId(image.getUser().getId());
        dto.setUsername(image.getUser().getUsername());
        return dto;
    }
}
