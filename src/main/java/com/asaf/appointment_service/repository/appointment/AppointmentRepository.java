package com.asaf.appointment_service.repository.appointment;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBusinessIdOrderByScheduledAtDesc(Long businessId);

    Page<Appointment> findByBusinessIdOrderByScheduledAtDesc(Long businessId, Pageable pageable);

    List<Appointment> findByBusinessIdAndRiskyTrue(Long businessId);

    List<Appointment> findByBusinessIdAndCustomerIdOrderByScheduledAtDesc(Long businessId, Long customerId);

    Page<Appointment> findByBusinessIdAndCustomerIdOrderByScheduledAtDesc(
        Long businessId, Long customerId, Pageable pageable);

    List<Appointment> findByBusinessIdAndEmployeeIdAndScheduledAtBetween(
        Long businessId,
        Long employeeId,
        LocalDateTime start,
        LocalDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.businessId = :businessId " +
           "AND a.employeeId = :employeeId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt < :end AND a.endAt > :start")
    List<Appointment> findConflictingAppointmentsForBusiness(
        @Param("businessId") Long businessId,
        @Param("employeeId") Long employeeId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.businessId = :businessId " +
           "AND a.employeeId = :employeeId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt < :end AND a.endAt > :start " +
           "AND a.id <> :excludeAppointmentId")
    List<Appointment> findConflictingAppointmentsForBusinessExcluding(
        @Param("businessId") Long businessId,
        @Param("employeeId") Long employeeId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("excludeAppointmentId") Long excludeAppointmentId
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

    @Query("SELECT a FROM Appointment a WHERE a.businessId = :businessId " +
           "AND a.scheduledAt BETWEEN :start AND :end " +
           "ORDER BY a.scheduledAt")
    Page<Appointment> findByBusinessIdAndDateRange(
        @Param("businessId") Long businessId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
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

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.customerId = :customerId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt >= :dayStart AND a.scheduledAt < :dayEnd " +
           "AND a.id <> :excludeAppointmentId")
    long countActiveByCustomerOnDateExcluding(
        @Param("customerId") Long customerId,
        @Param("dayStart") LocalDateTime dayStart,
        @Param("dayEnd") LocalDateTime dayEnd,
        @Param("excludeAppointmentId") Long excludeAppointmentId
    );

    @Query("SELECT a FROM Appointment a WHERE a.employeeId = :employeeId " +
           "AND a.status IN ('PENDING', 'CONFIRMED', 'RISKY') " +
           "AND a.scheduledAt < :end AND a.endAt > :start")
    List<Appointment> findActiveByEmployeeAndDateRange(
        @Param("employeeId") Long employeeId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    Page<Appointment> findByBusinessIdAndEmployeeIdOrderByScheduledAtDesc(
        Long businessId,
        Long employeeId,
        Pageable pageable
    );

    @Query("SELECT a FROM Appointment a WHERE a.businessId = :businessId " +
           "AND a.employeeId = :employeeId " +
           "AND a.scheduledAt BETWEEN :start AND :end " +
           "ORDER BY a.scheduledAt")
    Page<Appointment> findByBusinessIdAndEmployeeIdAndDateRange(
        @Param("businessId") Long businessId,
        @Param("employeeId") Long employeeId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    // ── Dashboard istatistik sorguları (B12) ──────────────────────────────────

    /**
     * Hizmet bazlı randevu sayımı. Dashboard top-services endpoint'i için.
     * Dönen dizi: [serviceId (Long), count (Long)]
     */
    @Query("SELECT a.serviceId, COUNT(a) as cnt FROM Appointment a " +
           "WHERE a.businessId = :businessId " +
           "AND a.scheduledAt BETWEEN :start AND :end " +
           "GROUP BY a.serviceId ORDER BY cnt DESC")
    List<Object[]> countByServiceIdForBusiness(
        @Param("businessId") Long businessId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    /**
     * Tamamlanan randevu sayısını hizmet bazlı sayar. Gelir hesabı için.
     * Dönen dizi: [serviceId (Long), completedCount (Long)]
     */
    @Query("SELECT a.serviceId, COUNT(a) as cnt FROM Appointment a " +
           "WHERE a.businessId = :businessId " +
           "AND a.status = 'COMPLETED' " +
           "AND a.scheduledAt BETWEEN :start AND :end " +
           "GROUP BY a.serviceId")
    List<Object[]> countCompletedByServiceIdForBusiness(
        @Param("businessId") Long businessId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Belirli bir dönemde birden fazla randevusu olan (tekrar eden) müşteri sayısı.
     */
    @Query("SELECT COUNT(DISTINCT a.customerId) FROM Appointment a " +
           "WHERE a.businessId = :businessId " +
           "AND a.scheduledAt BETWEEN :start AND :end " +
           "AND a.customerId IN (" +
           "  SELECT a2.customerId FROM Appointment a2 " +
           "  WHERE a2.businessId = :businessId " +
           "  AND a2.scheduledAt BETWEEN :start AND :end " +
           "  GROUP BY a2.customerId HAVING COUNT(a2) >= 2" +
           ")")
    long countReturningCustomers(
        @Param("businessId") Long businessId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Dönemde randevusu olan müşteri sayısı (unique).
     */
    @Query("SELECT COUNT(DISTINCT a.customerId) FROM Appointment a " +
           "WHERE a.businessId = :businessId " +
           "AND a.scheduledAt BETWEEN :start AND :end")
    long countUniqueCustomers(
        @Param("businessId") Long businessId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * En çok randevusu olan müşteriler. Dashboard top-customers için.
     * Dönen dizi: [customerId (Long), count (Long)]
     */
    @Query("SELECT a.customerId, COUNT(a) as cnt FROM Appointment a " +
           "WHERE a.businessId = :businessId " +
           "AND a.scheduledAt BETWEEN :start AND :end " +
           "GROUP BY a.customerId ORDER BY cnt DESC")
    List<Object[]> findTopCustomersByAppointmentCount(
        @Param("businessId") Long businessId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );
}
