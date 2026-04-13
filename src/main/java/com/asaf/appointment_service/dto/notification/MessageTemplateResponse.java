package com.asaf.appointment_service.dto.notification;

import com.asaf.appointment_service.entity.notification.MessageTemplateType;
import java.time.LocalDateTime;

public record MessageTemplateResponse(
    Long id,
    Long businessId,
    MessageTemplateType type,
    String name,
    String headerTemplate,
    String bodyTemplate,
    String footerTemplate,
    boolean showConfirmButton,
    boolean showCancelButton,
    boolean showRescheduleButton,
    String confirmButtonText,
    String cancelButtonText,
    String rescheduleButtonText,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
