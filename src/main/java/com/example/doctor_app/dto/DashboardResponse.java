package com.example.doctor_app.dto;

public record DashboardResponse(long doctorCount, long patientCount, long availableSlotCount, long bookedSlotCount) {
}
