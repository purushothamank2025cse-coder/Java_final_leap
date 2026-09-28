package com.example.doctor_app.dto;

import com.example.doctor_app.model.DoctorSlot;
import com.example.doctor_app.model.SlotStatus;

import java.time.LocalDateTime;

public record SlotResponse(Long id, Long doctorId, String doctorName, LocalDateTime startTime,
                           LocalDateTime endTime, SlotStatus status) {
    public static SlotResponse from(DoctorSlot slot) {
        return new SlotResponse(slot.getId(), slot.getDoctor().getId(), slot.getDoctor().getName(),
                slot.getStartTime(), slot.getEndTime(), slot.getStatus());
    }
}
