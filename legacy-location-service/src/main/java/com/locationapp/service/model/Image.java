package com.locationapp.service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Image Entity
 *
 * Represents images associated with locations.
 * Stores image metadata and file path/URL.
 */
@Entity
@Table(name = "images", indexes = {
    @Index(name = "idx_image_location", columnList = "location_id"),
    @Index(name = "idx_image_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 100)
    private String contentType; // e.g., image/jpeg, image/png

    @Column(nullable = false)
    private Long fileSize; // in bytes

    @Column(length = 500)
    private String caption;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrimary = false; // Main image for the location

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Helper methods
    public String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "";
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }
}
