package com.example.doctor_app.dto;

import com.example.doctor_app.model.Patient;

public record PatientResponse(Long id, String name, String email, String phone) {
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getName(), patient.getEmail(), patient.getPhone());
    }
}
