package com.asaf.appointment_service.repository.notification;

import com.asaf.appointment_service.entity.notification.NotificationLog;
import com.asaf.appointment_service.entity.notification.NotificationStatus;
import com.asaf.appointment_service.entity.notification.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Optional<NotificationLog> findTopByPhoneNumberAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
        String phoneNumber, List<NotificationType> types, LocalDateTime createdAfter);

    List<NotificationLog> findByBusinessIdAndCustomerIdOrderByCreatedAtDesc(Long businessId, Long customerId);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    boolean existsByAppointmentIdAndType(Long appointmentId, NotificationType type);

    long countByStatusInAndCreatedAtBetween(
        List<NotificationStatus> statuses, LocalDateTime from, LocalDateTime to);

    long countByBusinessIdAndStatusInAndCreatedAtBetween(
        Long businessId, List<NotificationStatus> statuses,
        LocalDateTime from, LocalDateTime to);

    @Query("SELECT n.businessId, COUNT(n) FROM NotificationLog n " +
           "WHERE n.status IN :statuses AND n.createdAt BETWEEN :from AND :to " +
           "GROUP BY n.businessId ORDER BY COUNT(n) DESC")
    List<Object[]> countByBusinessGrouped(
        @Param("statuses") List<NotificationStatus> statuses,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    @Query("SELECT n.type, COUNT(n) FROM NotificationLog n " +
           "WHERE n.status IN :statuses AND n.createdAt BETWEEN :from AND :to " +
           "GROUP BY n.type")
    List<Object[]> countByTypeGrouped(
        @Param("statuses") List<NotificationStatus> statuses,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', n.createdAt), COUNT(n) FROM NotificationLog n " +
           "WHERE n.status IN :statuses AND n.createdAt >= :from " +
           "GROUP BY FUNCTION('DATE', n.createdAt) " +
           "ORDER BY FUNCTION('DATE', n.createdAt)")
    List<Object[]> dailyCountSince(
        @Param("statuses") List<NotificationStatus> statuses,
        @Param("from") LocalDateTime from);
}
