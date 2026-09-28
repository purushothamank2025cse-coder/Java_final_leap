package com.example.doctor_app.repository;

import com.example.doctor_app.model.ClinicUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicUserRepository extends JpaRepository<ClinicUser, Long> {
    Optional<ClinicUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
