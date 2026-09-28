package com.example.doctor_app.dto;

import com.example.doctor_app.model.Doctor;

import java.util.Set;

public record DoctorResponse(Long id, String name, String email, Set<String> specializations, boolean active) {
    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getName(), doctor.getEmail(),
                doctor.getSpecializations(), doctor.isActive());
    }
}
