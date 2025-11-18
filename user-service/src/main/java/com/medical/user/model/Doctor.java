package com.medical.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "doctors", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_specialty", columnList = "specialty"),
    @Index(name = "idx_location", columnList = "country,city")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String specialty; // e.g., Cardiology, Neurology, etc.

    private String licenseNumber;

    private String institution;

    private String country;

    private String city;

    @Column(length = 1000)
    private String bio;

    private String profileImageUrl;

    private Integer yearsOfExperience;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "doctor_roles", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    private Boolean verified = false;

    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    // Statistics
    private Long totalPosts = 0L;
    private Long totalFollowers = 0L;
    private Long totalFollowing = 0L;

    @PrePersist
    protected void onCreate() {
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add("ROLE_DOCTOR");
        }
    }
}
