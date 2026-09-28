package com.example.doctor_app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private DoctorSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @Column
    private LocalDateTime cancelledAt;

    protected Appointment() {
    }

    public Appointment(Patient patient, DoctorSlot slot) {
        this.patient = patient;
        this.slot = slot;
        this.bookedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public DoctorSlot getSlot() { return slot; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
