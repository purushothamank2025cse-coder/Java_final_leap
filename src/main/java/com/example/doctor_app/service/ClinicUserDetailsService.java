package com.example.doctor_app.service;

import com.example.doctor_app.model.ClinicUser;
import com.example.doctor_app.repository.ClinicUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ClinicUserDetailsService implements UserDetailsService {
    private final ClinicUserRepository users;

    public ClinicUserDetailsService(ClinicUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        ClinicUser user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles("CLINIC_USER")
                .build();
    }
}
