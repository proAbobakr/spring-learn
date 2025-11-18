package com.medical.user.service;

import com.medical.common.dto.ApiResponse;
import com.medical.common.dto.PageResponse;
import com.medical.common.exception.BadRequestException;
import com.medical.common.exception.ResourceNotFoundException;
import com.medical.common.security.JwtUtil;
import com.medical.user.dto.AuthResponseDTO;
import com.medical.user.dto.DoctorDTO;
import com.medical.user.dto.LoginRequestDTO;
import com.medical.user.dto.RegisterRequestDTO;
import com.medical.user.model.Doctor;
import com.medical.user.model.Following;
import com.medical.user.repository.DoctorRepository;
import com.medical.user.repository.FollowingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final FollowingRepository followingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        Doctor doctor = new Doctor();
        doctor.setEmail(request.getEmail());
        doctor.setPassword(passwordEncoder.encode(request.getPassword()));
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setSpecialty(request.getSpecialty());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setInstitution(request.getInstitution());
        doctor.setCountry(request.getCountry());
        doctor.setCity(request.getCity());
        doctor.setBio(request.getBio());
        doctor.setYearsOfExperience(request.getYearsOfExperience());

        doctor = doctorRepository.save(doctor);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "DOCTOR_REGISTERED");
        event.put("doctorId", doctor.getId());
        event.put("email", doctor.getEmail());
        kafkaTemplate.send("user-events", event);

        String token = jwtUtil.generateToken(doctor.getEmail());
        return new AuthResponseDTO(token, convertToDTO(doctor));
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        Doctor doctor = doctorRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), doctor.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }

        if (!doctor.getActive()) {
            throw new BadRequestException("Account is inactive");
        }

        doctor.setLastLoginAt(LocalDateTime.now());
        doctorRepository.save(doctor);

        String token = jwtUtil.generateToken(doctor.getEmail());
        return new AuthResponseDTO(token, convertToDTO(doctor));
    }

    public DoctorDTO getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return convertToDTO(doctor);
    }

    public DoctorDTO getDoctorByEmail(String email) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return convertToDTO(doctor);
    }

    public PageResponse<DoctorDTO> searchDoctors(String query, Pageable pageable) {
        Page<Doctor> page = doctorRepository.searchDoctors(query, pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<DoctorDTO> getDoctorsBySpecialty(String specialty, Pageable pageable) {
        Page<Doctor> page = doctorRepository.findBySpecialty(specialty, pageable);
        return convertToPageResponse(page);
    }

    public PageResponse<DoctorDTO> getTopDoctors(Pageable pageable) {
        Page<Doctor> page = doctorRepository.findTopDoctors(pageable);
        return convertToPageResponse(page);
    }

    @Transactional
    public DoctorDTO updateDoctor(Long id, DoctorDTO dto) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (dto.getFirstName() != null) doctor.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) doctor.setLastName(dto.getLastName());
        if (dto.getSpecialty() != null) doctor.setSpecialty(dto.getSpecialty());
        if (dto.getBio() != null) doctor.setBio(dto.getBio());
        if (dto.getInstitution() != null) doctor.setInstitution(dto.getInstitution());
        if (dto.getCountry() != null) doctor.setCountry(dto.getCountry());
        if (dto.getCity() != null) doctor.setCity(dto.getCity());
        if (dto.getProfileImageUrl() != null) doctor.setProfileImageUrl(dto.getProfileImageUrl());
        if (dto.getYearsOfExperience() != null) doctor.setYearsOfExperience(dto.getYearsOfExperience());

        doctor = doctorRepository.save(doctor);
        return convertToDTO(doctor);
    }

    @Transactional
    public void followDoctor(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BadRequestException("Cannot follow yourself");
        }

        if (followingRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BadRequestException("Already following this doctor");
        }

        Doctor follower = doctorRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower not found"));
        Doctor following = doctorRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor to follow not found"));

        Following followRelation = new Following();
        followRelation.setFollowerId(followerId);
        followRelation.setFollowingId(followingId);
        followingRepository.save(followRelation);

        // Update counts
        follower.setTotalFollowing(follower.getTotalFollowing() + 1);
        following.setTotalFollowers(following.getTotalFollowers() + 1);
        doctorRepository.save(follower);
        doctorRepository.save(following);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "DOCTOR_FOLLOWED");
        event.put("followerId", followerId);
        event.put("followingId", followingId);
        kafkaTemplate.send("user-events", event);
    }

    @Transactional
    public void unfollowDoctor(Long followerId, Long followingId) {
        Following following = followingRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Not following this doctor"));

        followingRepository.delete(following);

        // Update counts
        Doctor follower = doctorRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower not found"));
        Doctor followedDoctor = doctorRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        follower.setTotalFollowing(Math.max(0, follower.getTotalFollowing() - 1));
        followedDoctor.setTotalFollowers(Math.max(0, followedDoctor.getTotalFollowers() - 1));
        doctorRepository.save(follower);
        doctorRepository.save(followedDoctor);
    }

    public PageResponse<DoctorDTO> getFollowers(Long doctorId, Pageable pageable) {
        Page<Following> followingPage = followingRepository.findByFollowingId(doctorId, pageable);
        Page<Doctor> doctorPage = followingPage.map(f ->
            doctorRepository.findById(f.getFollowerId()).orElse(null));
        return convertToPageResponse(doctorPage);
    }

    public PageResponse<DoctorDTO> getFollowing(Long doctorId, Pageable pageable) {
        Page<Following> followingPage = followingRepository.findByFollowerId(doctorId, pageable);
        Page<Doctor> doctorPage = followingPage.map(f ->
            doctorRepository.findById(f.getFollowingId()).orElse(null));
        return convertToPageResponse(doctorPage);
    }

    private DoctorDTO convertToDTO(Doctor doctor) {
        DoctorDTO dto = new DoctorDTO();
        dto.setId(doctor.getId());
        dto.setEmail(doctor.getEmail());
        dto.setFirstName(doctor.getFirstName());
        dto.setLastName(doctor.getLastName());
        dto.setSpecialty(doctor.getSpecialty());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setInstitution(doctor.getInstitution());
        dto.setCountry(doctor.getCountry());
        dto.setCity(doctor.getCity());
        dto.setBio(doctor.getBio());
        dto.setProfileImageUrl(doctor.getProfileImageUrl());
        dto.setYearsOfExperience(doctor.getYearsOfExperience());
        dto.setVerified(doctor.getVerified());
        dto.setTotalPosts(doctor.getTotalPosts());
        dto.setTotalFollowers(doctor.getTotalFollowers());
        dto.setTotalFollowing(doctor.getTotalFollowing());
        dto.setCreatedAt(doctor.getCreatedAt());
        return dto;
    }

    private PageResponse<DoctorDTO> convertToPageResponse(Page<Doctor> page) {
        PageResponse<DoctorDTO> response = new PageResponse<>();
        response.setContent(page.getContent().stream().map(this::convertToDTO).toList());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        return response;
    }
}
