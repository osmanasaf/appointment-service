package com.asaf.appointment_service.mapper.review;

import com.asaf.appointment_service.dto.review.PublicReviewItemResponse;
import com.asaf.appointment_service.dto.review.ReviewResponse;
import com.asaf.appointment_service.entity.review.Review;

public class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewResponse toResponse(Review entity) {
        return new ReviewResponse(
            entity.getId(),
            entity.getBusinessId(),
            entity.getAppointmentId(),
            entity.getCustomerId(),
            entity.getEmployeeId(),
            entity.getRating(),
            entity.getComment(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }

    public static PublicReviewItemResponse toPublicItem(Review entity, String customerName) {
        return new PublicReviewItemResponse(
            maskName(customerName),
            entity.getRating(),
            entity.getComment(),
            entity.getCreatedAt()
        );
    }

    private static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Müşteri";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }
}
