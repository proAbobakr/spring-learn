package com.medical.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String specialty;
    private String licenseNumber;
    private String institution;
    private String country;
    private String city;
    private String bio;
    private String profileImageUrl;
    private Integer yearsOfExperience;
    private Boolean verified;
    private Long totalPosts;
    private Long totalFollowers;
    private Long totalFollowing;
    private LocalDateTime createdAt;
}
