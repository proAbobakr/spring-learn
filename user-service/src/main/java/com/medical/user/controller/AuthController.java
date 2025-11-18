package com.medical.user.controller;

import com.medical.common.dto.ApiResponse;
import com.medical.user.dto.AuthResponseDTO;
import com.medical.user.dto.LoginRequestDTO;
import com.medical.user.dto.RegisterRequestDTO;
import com.medical.user.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DoctorService doctorService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = doctorService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = doctorService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
