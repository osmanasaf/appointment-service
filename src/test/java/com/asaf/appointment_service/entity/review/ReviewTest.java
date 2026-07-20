package com.asaf.appointment_service.entity.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewTest {

    private Review.Builder validBuilder() {
        return Review.builder().businessId(1L).appointmentId(10L).customerId(5L).rating(4);
    }

    @Test
    void should_createWithPendingApprovalStatus_when_built() {
        Review review = validBuilder().build();

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING_APPROVAL);
        assertThat(review.getRating()).isEqualTo(4);
    }

    @Test
    void should_throwIllegalArgument_when_ratingBelowOne() {
        assertThatThrownBy(() -> validBuilder().rating(0).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throwIllegalArgument_when_ratingAboveFive() {
        assertThatThrownBy(() -> validBuilder().rating(6).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_transitionToPublished_when_publishCalled() {
        Review review = validBuilder().build();

        review.publish();

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void should_transitionToRejected_when_rejectCalled() {
        Review review = validBuilder().build();

        review.reject();

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REJECTED);
    }

    @Test
    void should_returnTrue_when_ratingIsFive() {
        Review review = validBuilder().rating(5).build();

        assertThat(review.isFiveStars()).isTrue();
    }

    @Test
    void should_returnFalse_when_ratingIsNotFive() {
        Review review = validBuilder().rating(4).build();

        assertThat(review.isFiveStars()).isFalse();
    }
}
