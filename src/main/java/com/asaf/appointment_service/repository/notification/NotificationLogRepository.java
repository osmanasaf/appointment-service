package com.asaf.appointment_service.repository.notification;

import com.asaf.appointment_service.entity.notification.NotificationLog;
import com.asaf.appointment_service.entity.notification.NotificationStatus;
import com.asaf.appointment_service.entity.notification.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Optional<NotificationLog> findTopByPhoneNumberAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
        String phoneNumber, List<NotificationType> types, LocalDateTime createdAfter);

    List<NotificationLog> findByAppointmentId(Long appointmentId);

    List<NotificationLog> findByCustomerId(Long customerId);

    List<NotificationLog> findByStatus(NotificationStatus status);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    Optional<NotificationLog> findByExternalId(String externalId);

    List<NotificationLog> findByBusinessIdAndCreatedAtBetween(
        Long businessId, LocalDateTime start, LocalDateTime end);

    boolean existsByAppointmentIdAndType(Long appointmentId, NotificationType type);
}
