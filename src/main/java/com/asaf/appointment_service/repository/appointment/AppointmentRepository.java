package com.asaf.appointment_service.repository.appointment;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBusinessIdOrderByScheduledAtDesc(Long businessId);

    List<Appointment> findByBusinessIdAndStatus(Long businessId, AppointmentStatus status);

    List<Appointment> findByBusinessIdAndRiskyTrue(Long businessId);

    List<Appointment> findByCustomerId(Long customerId);

    List<Appointment> findByEmployeeIdAndScheduledAtBetween(
        Long employeeId, 
        LocalDateTime start, 
        LocalDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.employeeId = :employeeId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt < :end AND a.endAt > :start")
    List<Appointment> findConflictingAppointments(
        @Param("employeeId") Long employeeId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.employeeId = :employeeId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt > :now ORDER BY a.scheduledAt")
    List<Appointment> findFutureActiveByEmployeeId(
        @Param("employeeId") Long employeeId,
        @Param("now") LocalDateTime now
    );

    @Query("SELECT a FROM Appointment a WHERE a.businessId = :businessId " +
           "AND a.scheduledAt BETWEEN :start AND :end " +
           "ORDER BY a.scheduledAt")
    List<Appointment> findByBusinessIdAndDateRange(
        @Param("businessId") Long businessId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.reminderSent = false " +
           "AND a.status IN ('PENDING', 'CONFIRMED') " +
           "AND a.scheduledAt BETWEEN :start AND :end")
    List<Appointment> findAppointmentsNeedingReminder(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.customerId = :customerId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt < :end AND a.endAt > :start")
    List<Appointment> findActiveByCustomerAndTimeOverlap(
        @Param("customerId") Long customerId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.customerId = :customerId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt >= :dayStart AND a.scheduledAt < :dayEnd")
    long countActiveByCustomerOnDate(
        @Param("customerId") Long customerId,
        @Param("dayStart") LocalDateTime dayStart,
        @Param("dayEnd") LocalDateTime dayEnd
    );
}
