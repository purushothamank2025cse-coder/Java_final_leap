package com.example.doctor_app.repository;

import com.example.doctor_app.model.DoctorSlot;
import com.example.doctor_app.model.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long> {
    List<DoctorSlot> findByDoctorId(Long doctorId);
    List<DoctorSlot> findByDoctorIdAndStartTimeBetween(Long doctorId, LocalDateTime from, LocalDateTime to);
    List<DoctorSlot> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);
    boolean existsByDoctorIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorId, LocalDateTime endTime, LocalDateTime startTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSlot s where s.id = :id")
    Optional<DoctorSlot> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(SlotStatus status);
}
