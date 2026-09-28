package com.example.doctor_app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record PublishSlotsRequest(
        @NotNull LocalDate dateFrom,
        @NotNull LocalDate dateTo,
        @NotNull LocalTime timeFrom,
        @NotNull LocalTime timeTo,
        @Positive int slotMinutes,
        Set<DayOfWeek> daysOfWeek) {
}
