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

    List<NotificationLog> findByBusinessIdAndCustomerIdOrderByCreatedAtDesc(Long businessId, Long customerId);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    boolean existsByAppointmentIdAndType(Long appointmentId, NotificationType type);
}
