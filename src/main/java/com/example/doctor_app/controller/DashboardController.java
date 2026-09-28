package com.example.doctor_app.controller;

import com.example.doctor_app.dto.DashboardResponse;
import com.example.doctor_app.repository.PatientRepository;
import com.example.doctor_app.repository.DoctorSlotRepository;
import com.example.doctor_app.service.AppointmentService;
import com.example.doctor_app.service.DoctorService;
import com.example.doctor_app.model.SlotStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DoctorService doctorService;
    private final PatientRepository patientRepository;
    private final DoctorSlotRepository slotRepository;

    public DashboardController(DoctorService doctorService, PatientRepository patientRepository,
                               DoctorSlotRepository slotRepository) {
        this.doctorService = doctorService;
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
    }

    @GetMapping
    public DashboardResponse summary() {
        return new DashboardResponse(doctorService.count(), patientRepository.count(),
                slotRepository.countByStatus(SlotStatus.AVAILABLE),
                slotRepository.countByStatus(SlotStatus.BOOKED));
    }
}
