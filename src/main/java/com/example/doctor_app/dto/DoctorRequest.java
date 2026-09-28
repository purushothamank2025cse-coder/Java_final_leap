package com.example.doctor_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record DoctorRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotEmpty Set<@NotBlank String> specializations) {
}
