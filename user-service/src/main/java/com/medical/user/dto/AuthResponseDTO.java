package com.medical.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String type = "Bearer";
    private DoctorDTO doctor;

    public AuthResponseDTO(String token, DoctorDTO doctor) {
        this.token = token;
        this.doctor = doctor;
    }
}
