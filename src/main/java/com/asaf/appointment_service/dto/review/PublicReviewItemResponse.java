package com.asaf.appointment_service.dto.review;

import java.time.LocalDateTime;

public record PublicReviewItemResponse(
    String maskedCustomerName,
    int rating,
    String comment,
    LocalDateTime createdAt
) {
}
