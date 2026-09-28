package com.example.doctor_app.service;

import com.example.doctor_app.dto.BookAppointmentRequest;
import com.example.doctor_app.exception.ApiException;
import com.example.doctor_app.model.Appointment;
import com.example.doctor_app.model.AppointmentStatus;
import com.example.doctor_app.model.DoctorSlot;
import com.example.doctor_app.model.SlotStatus;
import com.example.doctor_app.repository.AppointmentRepository;
import com.example.doctor_app.repository.DoctorSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorSlotRepository slotRepository;
    private final PatientService patientService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorSlotRepository slotRepository,
                              PatientService patientService) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.patientService = patientService;
    }

    public Appointment book(BookAppointmentRequest request) {
        var patient = patientService.findPatient(request.patientId());
        DoctorSlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Slot " + request.slotId() + " was not found"));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "This slot is already booked");
        }
        if (!slot.getDoctor().isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "This doctor's schedule is no longer accepting bookings");
        }
        if (!slot.getStartTime().isAfter(java.time.LocalDateTime.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "A past or currently-started slot cannot be booked");
        }
        slot.book();
        return appointmentRepository.save(new Appointment(patient, slot));
    }

    public Appointment cancel(Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Appointment " + appointmentId + " was not found"));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This appointment has already been cancelled");
        }
        DoctorSlot slot = slotRepository.findByIdForUpdate(appointment.getSlot().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "The appointment slot was not found"));
        appointment.cancel();
        slot.makeAvailable();
        return appointment;
    }

    @Transactional(readOnly = true)
    public List<Appointment> bookedAppointments(LocalDate date) {
        if (date == null) {
            return appointmentRepository.findByStatusOrderBySlotStartTime(AppointmentStatus.BOOKED);
        }
        LocalDateTime from = date.atStartOfDay();
        return appointmentRepository
                .findByStatusAndSlotStartTimeGreaterThanEqualAndSlotStartTimeLessThanOrderBySlotStartTime(
                        AppointmentStatus.BOOKED, from, date.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public List<Appointment> todaysAppointments(Long doctorId) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findBySlotDoctorIdAndSlotStartTimeBetweenAndStatusOrderBySlotStartTime(
                doctorId, today.atStartOfDay(), today.plusDays(1).atStartOfDay(), AppointmentStatus.BOOKED);
    }

    @Transactional(readOnly = true)
    public long bookedCount() {
        return slotRepository.countByStatus(SlotStatus.BOOKED);
    }
}
