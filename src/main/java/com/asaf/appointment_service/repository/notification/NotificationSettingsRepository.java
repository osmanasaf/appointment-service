package com.asaf.appointment_service.repository.notification;

import com.asaf.appointment_service.entity.notification.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByBusinessId(Long businessId);

    boolean existsByBusinessId(Long businessId);
}
