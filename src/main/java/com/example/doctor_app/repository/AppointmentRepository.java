package com.example.doctor_app.repository;

import com.example.doctor_app.model.Appointment;
import com.example.doctor_app.model.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByStatusOrderBySlotStartTime(AppointmentStatus status);

    List<Appointment> findByStatusAndSlotStartTimeGreaterThanEqualAndSlotStartTimeLessThanOrderBySlotStartTime(
            AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    List<Appointment> findBySlotDoctorIdAndSlotStartTimeBetweenAndStatusOrderBySlotStartTime(
            Long doctorId, LocalDateTime from, LocalDateTime to, AppointmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);
}
