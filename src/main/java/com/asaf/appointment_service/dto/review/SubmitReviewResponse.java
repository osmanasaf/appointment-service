package com.asaf.appointment_service.dto.review;

public record SubmitReviewResponse(
    Long reviewId,
    int rating,
    String googleReviewUrl
) {
}
