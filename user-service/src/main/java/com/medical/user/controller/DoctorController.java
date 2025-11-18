package com.medical.user.controller;

import com.medical.common.dto.ApiResponse;
import com.medical.common.dto.PageResponse;
import com.medical.user.dto.DoctorDTO;
import com.medical.user.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorDTO>> getDoctorById(@PathVariable Long id) {
        DoctorDTO doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success(doctor));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<DoctorDTO>> getDoctorByEmail(@PathVariable String email) {
        DoctorDTO doctor = doctorService.getDoctorByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(doctor));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<DoctorDTO>>> searchDoctors(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<DoctorDTO> doctors = doctorService.searchDoctors(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/specialty/{specialty}")
    public ResponseEntity<ApiResponse<PageResponse<DoctorDTO>>> getDoctorsBySpecialty(
            @PathVariable String specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<DoctorDTO> doctors = doctorService.getDoctorsBySpecialty(specialty, pageable);
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<PageResponse<DoctorDTO>>> getTopDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<DoctorDTO> doctors = doctorService.getTopDoctors(pageable);
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorDTO>> updateDoctor(
            @PathVariable Long id,
            @RequestBody DoctorDTO dto) {
        DoctorDTO updated = doctorService.updateDoctor(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully", updated));
    }

    @PostMapping("/{followerId}/follow/{followingId}")
    public ResponseEntity<ApiResponse<Void>> followDoctor(
            @PathVariable Long followerId,
            @PathVariable Long followingId) {
        doctorService.followDoctor(followerId, followingId);
        return ResponseEntity.ok(ApiResponse.success("Followed successfully", null));
    }

    @DeleteMapping("/{followerId}/unfollow/{followingId}")
    public ResponseEntity<ApiResponse<Void>> unfollowDoctor(
            @PathVariable Long followerId,
            @PathVariable Long followingId) {
        doctorService.unfollowDoctor(followerId, followingId);
        return ResponseEntity.ok(ApiResponse.success("Unfollowed successfully", null));
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<ApiResponse<PageResponse<DoctorDTO>>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<DoctorDTO> followers = doctorService.getFollowers(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(followers));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<ApiResponse<PageResponse<DoctorDTO>>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<DoctorDTO> following = doctorService.getFollowing(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(following));
    }
}
