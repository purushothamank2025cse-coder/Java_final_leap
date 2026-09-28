package com.example.doctor_app.service;

import com.example.doctor_app.dto.DoctorRequest;
import com.example.doctor_app.exception.ApiException;
import com.example.doctor_app.model.Doctor;
import com.example.doctor_app.repository.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class DoctorService {
    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public Doctor create(DoctorRequest request) {
        if (doctorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "A doctor with this email already exists");
        }
        return doctorRepository.save(new Doctor(request.name().trim(), request.email().trim(),
                cleanSpecializations(request.specializations())));
    }

    public Doctor update(Long id, DoctorRequest request) {
        Doctor doctor = findDoctor(id);
        boolean emailUsedByAnotherDoctor = doctorRepository.existsByEmailIgnoreCase(request.email())
                && !doctor.getEmail().equalsIgnoreCase(request.email());
        if (emailUsedByAnotherDoctor) {
            throw new ApiException(HttpStatus.CONFLICT, "A doctor with this email already exists");
        }
        doctor.update(request.name().trim(), request.email().trim(), cleanSpecializations(request.specializations()));
        return doctor;
    }

    @Transactional(readOnly = true)
    public List<Doctor> list() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Doctor get(Long id) {
        return findDoctor(id);
    }

    public void deactivate(Long id) {
        findDoctor(id).deactivate();
    }

    @Transactional(readOnly = true)
    public long count() {
        return doctorRepository.count();
    }

    private Doctor findDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor " + id + " was not found"));
    }

    private Set<String> cleanSpecializations(Set<String> specializations) {
        return specializations.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }
}
