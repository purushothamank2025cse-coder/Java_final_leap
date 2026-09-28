package com.example.doctor_app.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "doctor_specializations", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "specialization", nullable = false)
    private Set<String> specializations = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    protected Doctor() {
    }

    public Doctor(String name, String email, Set<String> specializations) {
        this.name = name;
        this.email = email;
        this.specializations = new HashSet<>(specializations);
    }

    public void update(String name, String email, Set<String> specializations) {
        this.name = name;
        this.email = email;
        this.specializations.clear();
        this.specializations.addAll(specializations);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Set<String> getSpecializations() { return Set.copyOf(specializations); }
    public boolean isActive() { return active; }
}
