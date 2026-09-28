package com.example.doctor_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PatientRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phone) {
}
