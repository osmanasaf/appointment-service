package com.asaf.appointment_service.dto.review;

import com.asaf.appointment_service.entity.review.ReviewStatus;
import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long businessId,
    Long appointmentId,
    Long customerId,
    Long employeeId,
    int rating,
    String comment,
    ReviewStatus status,
    LocalDateTime createdAt
) {
}
