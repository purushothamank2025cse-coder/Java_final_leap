package com.example.doctor_app.service;

import com.example.doctor_app.dto.PublishSlotsRequest;
import com.example.doctor_app.exception.ApiException;
import com.example.doctor_app.model.Doctor;
import com.example.doctor_app.model.DoctorSlot;
import com.example.doctor_app.model.SlotStatus;
import com.example.doctor_app.repository.DoctorRepository;
import com.example.doctor_app.repository.DoctorSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class SlotService {
    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository slotRepository;

    public SlotService(DoctorRepository doctorRepository, DoctorSlotRepository slotRepository) {
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
    }

    public List<DoctorSlot> publish(Long doctorId, PublishSlotsRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor " + doctorId + " was not found"));
        if (!doctor.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Inactive doctors cannot publish slots");
        }
        if (request.dateTo().isBefore(request.dateFrom())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dateTo must be on or after dateFrom");
        }
        if (!request.timeTo().isAfter(request.timeFrom())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "timeTo must be after timeFrom");
        }

        Set<DayOfWeek> days = request.daysOfWeek() == null || request.daysOfWeek().isEmpty()
                ? Set.of(DayOfWeek.values()) : request.daysOfWeek();
        List<DoctorSlot> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (LocalDate date = request.dateFrom(); !date.isAfter(request.dateTo()); date = date.plusDays(1)) {
            if (!days.contains(date.getDayOfWeek())) {
                continue;
            }
            LocalTime time = request.timeFrom();
            while (!time.plusMinutes(request.slotMinutes()).isAfter(request.timeTo())) {
                LocalDateTime start = LocalDateTime.of(date, time);
                LocalDateTime end = start.plusMinutes(request.slotMinutes());
                if (!start.isAfter(now)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot publish slots in the past");
                }
                if (slotRepository.existsByDoctorIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        doctorId, end, start)) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "A published slot overlaps the requested time " + start);
                }
                slots.add(new DoctorSlot(doctor, start, end));
                time = time.plusMinutes(request.slotMinutes());
            }
        }
        if (slots.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The date range and weekdays did not produce any complete future slots");
        }
        return slotRepository.saveAll(slots);
    }

    @Transactional(readOnly = true)
    public List<DoctorSlot> search(LocalDate date, String specialization) {
        LocalDateTime now = LocalDateTime.now();
        List<DoctorSlot> slots = date == null
                ? slotRepository.findAll()
                : slotRepository.findByStartTimeBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        return slots.stream()
                .filter(slot -> slot.getStatus() == SlotStatus.AVAILABLE)
                .filter(slot -> slot.getStartTime().isAfter(now))
                .filter(slot -> slot.getDoctor().isActive())
                .filter(slot -> specialization == null || specialization.isBlank()
                        || slot.getDoctor().getSpecializations().stream()
                        .anyMatch(value -> value.equalsIgnoreCase(specialization.trim())))
                .toList();
    }
}
