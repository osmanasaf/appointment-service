package com.asaf.appointment_service.repository.notification;

import com.asaf.appointment_service.entity.notification.MessageTemplate;
import com.asaf.appointment_service.entity.notification.MessageTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    List<MessageTemplate> findByBusinessIdAndActiveTrue(Long businessId);

    Optional<MessageTemplate> findByBusinessIdAndTypeAndActiveTrue(Long businessId, MessageTemplateType type);

    Optional<MessageTemplate> findByBusinessIdAndType(Long businessId, MessageTemplateType type);

    boolean existsByBusinessIdAndType(Long businessId, MessageTemplateType type);
}
