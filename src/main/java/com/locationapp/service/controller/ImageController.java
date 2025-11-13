package com.locationapp.service.controller;

import com.locationapp.service.dto.ImageResponseDTO;
import com.locationapp.service.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Image Controller
 *
 * REST endpoints for image upload and retrieval.
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Image upload and management endpoints")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Upload an image", description = "Uploads an image for a location")
    public ResponseEntity<ImageResponseDTO> uploadImage(
            @RequestParam Long locationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) Boolean isPrimary
    ) throws IOException {
        ImageResponseDTO response = imageService.uploadImage(locationId, file, caption, isPrimary);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get image metadata", description = "Returns image details without file content")
    public ResponseEntity<ImageResponseDTO> getImage(@PathVariable Long id) {
        ImageResponseDTO response = imageService.getImageById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/location/{locationId}")
    @Operation(summary = "Get images for location", description = "Returns all images for a specific location")
    public ResponseEntity<List<ImageResponseDTO>> getImagesForLocation(@PathVariable Long locationId) {
        List<ImageResponseDTO> images = imageService.getImagesForLocation(locationId);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/view/{filename}")
    @Operation(summary = "View image file", description = "Returns the actual image file")
    public ResponseEntity<Resource> viewImage(@PathVariable String filename) throws IOException {
        Path filePath = imageService.getImageFile(filename);
        Resource resource = new UrlResource(filePath.toUri());

        String contentType = "image/jpeg";
        if (filename.endsWith(".png")) {
            contentType = "image/png";
        } else if (filename.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (filename.endsWith(".webp")) {
            contentType = "image/webp";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update image metadata", description = "Updates image caption or primary status (owner only)")
    public ResponseEntity<ImageResponseDTO> updateImage(
            @PathVariable Long id,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) Boolean isPrimary
    ) {
        ImageResponseDTO response = imageService.updateImage(id, caption, isPrimary);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete image", description = "Deletes an image (owner only)")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) throws IOException {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
