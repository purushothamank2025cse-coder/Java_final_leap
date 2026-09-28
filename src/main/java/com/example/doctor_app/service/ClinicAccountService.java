package com.example.doctor_app.service;

import com.example.doctor_app.dto.SignUpRequest;
import com.example.doctor_app.exception.ApiException;
import com.example.doctor_app.model.ClinicUser;
import com.example.doctor_app.repository.ClinicUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClinicAccountService {
    private final ClinicUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public ClinicAccountService(ClinicUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public ClinicUser signUp(SignUpRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        return users.save(new ClinicUser(request.name().trim(), email,
                passwordEncoder.encode(request.password()), "USER"));
    }

    @Transactional(readOnly = true)
    public ClinicUser findByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated clinic account was not found"));
    }
}
