package com.asaf.appointment_service.controller.review;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.review.ReviewResponse;
import com.asaf.appointment_service.entity.review.Review;
import com.asaf.appointment_service.entity.review.ReviewStatus;
import com.asaf.appointment_service.mapper.review.ReviewMapper;
import com.asaf.appointment_service.service.review.ReviewService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> list(
            @RequestParam(required = false) ReviewStatus status) {
        Long businessId = currentUserService.getCurrentBusinessId();
        List<Review> reviews = reviewService.listByBusiness(businessId, status);
        return ResponseEntity.ok(ApiResponse.success(reviews.stream().map(ReviewMapper::toResponse).toList()));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable Long id) {
        Long businessId = currentUserService.getCurrentBusinessId();
        reviewService.publish(businessId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Değerlendirme yayınlandı"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Long id) {
        Long businessId = currentUserService.getCurrentBusinessId();
        reviewService.reject(businessId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Değerlendirme reddedildi"));
    }
}
