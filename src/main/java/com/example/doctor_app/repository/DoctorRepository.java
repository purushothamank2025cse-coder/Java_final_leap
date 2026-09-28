package com.example.doctor_app.repository;

import com.example.doctor_app.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    boolean existsByEmailIgnoreCase(String email);
    long countByActiveTrue();
}
