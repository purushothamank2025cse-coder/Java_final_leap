package com.example.doctor_app.dto;

import com.example.doctor_app.model.ClinicUser;

public record AuthResponse(String name, String email) {
    public static AuthResponse from(ClinicUser user) {
        return new AuthResponse(user.getName(), user.getEmail());
    }
}
