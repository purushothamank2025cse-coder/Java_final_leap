package com.example.doctor_app.controller;

import com.example.doctor_app.dto.AppointmentResponse;
import com.example.doctor_app.dto.DoctorRequest;
import com.example.doctor_app.dto.DoctorResponse;
import com.example.doctor_app.dto.SlotResponse;
import com.example.doctor_app.repository.DoctorSlotRepository;
import com.example.doctor_app.service.AppointmentService;
import com.example.doctor_app.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final DoctorSlotRepository slotRepository;

    public DoctorController(DoctorService doctorService, AppointmentService appointmentService,
                            DoctorSlotRepository slotRepository) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.slotRepository = slotRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponse create(@Valid @RequestBody DoctorRequest request) {
        return DoctorResponse.from(doctorService.create(request));
    }

    @GetMapping
    public List<DoctorResponse> list() {
        return doctorService.list().stream().map(DoctorResponse::from).toList();
    }

    @GetMapping("/{doctorId}")
    public DoctorResponse get(@PathVariable Long doctorId) {
        return DoctorResponse.from(doctorService.get(doctorId));
    }

    @PutMapping("/{doctorId}")
    public DoctorResponse update(@PathVariable Long doctorId, @Valid @RequestBody DoctorRequest request) {
        return DoctorResponse.from(doctorService.update(doctorId, request));
    }

    @DeleteMapping("/{doctorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long doctorId) {
        doctorService.deactivate(doctorId);
    }

    @GetMapping("/{doctorId}/slots")
    public List<SlotResponse> slots(@PathVariable Long doctorId) {
        doctorService.get(doctorId);
        return slotRepository.findByDoctorId(doctorId).stream().map(SlotResponse::from).toList();
    }

    @GetMapping("/{doctorId}/appointments/today")
    public List<AppointmentResponse> todaysAppointments(@PathVariable Long doctorId) {
        doctorService.get(doctorId);
        return appointmentService.todaysAppointments(doctorId).stream()
                .map(AppointmentResponse::from).toList();
    }
}
