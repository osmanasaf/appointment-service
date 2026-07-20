package com.asaf.appointment_service.mapper.review;

import com.asaf.appointment_service.dto.review.PublicReviewItemResponse;
import com.asaf.appointment_service.entity.review.Review;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewMapperTest {

    private Review review(int rating, String comment) {
        return Review.builder().businessId(1L).appointmentId(10L).customerId(5L)
            .rating(rating).comment(comment).build();
    }

    @Test
    void should_maskLastNameToInitial_when_customerHasFirstAndLastName() {
        PublicReviewItemResponse response = ReviewMapper.toPublicItem(review(5, "Harika!"), "Ayşe Kaya");

        assertThat(response.maskedCustomerName()).isEqualTo("Ayşe K.");
    }

    @Test
    void should_returnNameAsIs_when_customerHasSingleName() {
        PublicReviewItemResponse response = ReviewMapper.toPublicItem(review(4, null), "Ayşe");

        assertThat(response.maskedCustomerName()).isEqualTo("Ayşe");
    }

    @Test
    void should_returnGenericLabel_when_customerNameIsNullOrBlank() {
        assertThat(ReviewMapper.toPublicItem(review(3, null), null).maskedCustomerName()).isEqualTo("Müşteri");
        assertThat(ReviewMapper.toPublicItem(review(3, null), "  ").maskedCustomerName()).isEqualTo("Müşteri");
    }

    @Test
    void should_maskOnlyLastToken_when_customerHasMultipleNames() {
        PublicReviewItemResponse response = ReviewMapper.toPublicItem(review(5, null), "Ayşe Nur Kaya");

        assertThat(response.maskedCustomerName()).isEqualTo("Ayşe K.");
    }
}
