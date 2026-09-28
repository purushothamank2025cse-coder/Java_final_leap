package com.example.doctor_app.dto;

import com.example.doctor_app.model.Appointment;
import com.example.doctor_app.model.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(Long id, AppointmentStatus status, Long patientId, String patientName,
                                  Long doctorId, String doctorName, LocalDateTime startTime,
                                  LocalDateTime endTime, LocalDateTime bookedAt) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getStatus(),
                appointment.getPatient().getId(), appointment.getPatient().getName(),
                appointment.getSlot().getDoctor().getId(), appointment.getSlot().getDoctor().getName(),
                appointment.getSlot().getStartTime(), appointment.getSlot().getEndTime(),
                appointment.getBookedAt());
    }
}
