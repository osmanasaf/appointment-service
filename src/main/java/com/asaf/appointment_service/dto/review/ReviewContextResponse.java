package com.asaf.appointment_service.dto.review;

import java.time.LocalDateTime;

public record ReviewContextResponse(
    Long appointmentId,
    String businessName,
    String serviceName,
    LocalDateTime scheduledAt,
    boolean alreadySubmitted
) {
}
