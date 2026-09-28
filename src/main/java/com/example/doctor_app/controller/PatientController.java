package com.example.doctor_app.controller;

import com.example.doctor_app.dto.PatientRequest;
import com.example.doctor_app.dto.PatientResponse;
import com.example.doctor_app.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody PatientRequest request) {
        return PatientResponse.from(patientService.create(request));
    }

    @PutMapping("/{patientId}")
    public PatientResponse update(@PathVariable Long patientId, @Valid @RequestBody PatientRequest request) {
        return PatientResponse.from(patientService.update(patientId, request));
    }

    @GetMapping
    public List<PatientResponse> list() {
        return patientService.list().stream().map(PatientResponse::from).toList();
    }
}
