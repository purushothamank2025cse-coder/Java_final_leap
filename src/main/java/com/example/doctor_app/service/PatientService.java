package com.example.doctor_app.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.doctor_app.dto.PatientRequest;
import com.example.doctor_app.exception.ApiException;
import com.example.doctor_app.model.Patient;
import com.example.doctor_app.repository.PatientRepository;

@Service
@Transactional
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient create(PatientRequest request) {
        if (patientRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "A patient with this email already exists");
        }
        return patientRepository.save(new Patient(request.name().trim(), request.email().trim(), request.phone().trim()));
    }

    public Patient update(Long id, PatientRequest request) {
        Patient patient = findPatient(id);
        String email = request.email().trim();
        if (patientRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "A patient with this email already exists");
        }
        patient.update(request.name().trim(), email, request.phone().trim());
        return patient;
    }

    @Transactional(readOnly = true)
    public List<Patient> list() {
        return patientRepository.findAll();
    }

    Patient findPatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient " + id + " was not found"));
    }
}

