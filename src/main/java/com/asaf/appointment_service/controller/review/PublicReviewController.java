package com.asaf.appointment_service.controller.review;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.review.PublicReviewItemResponse;
import com.asaf.appointment_service.dto.review.ReviewContextResponse;
import com.asaf.appointment_service.dto.review.SubmitReviewRequest;
import com.asaf.appointment_service.dto.review.SubmitReviewResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.review.Review;
import com.asaf.appointment_service.mapper.review.ReviewMapper;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.review.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicReviewController {

    private final ReviewService reviewService;
    private final BusinessService businessService;
    private final CustomerService customerService;

    public PublicReviewController(ReviewService reviewService, BusinessService businessService,
                                  CustomerService customerService) {
        this.reviewService = reviewService;
        this.businessService = businessService;
        this.customerService = customerService;
    }

    @GetMapping("/review/{token}")
    public ResponseEntity<ApiResponse<ReviewContextResponse>> getContext(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getContext(token)));
    }

    @PostMapping("/review/{token}")
    public ResponseEntity<ApiResponse<SubmitReviewResponse>> submit(
            @PathVariable String token,
            @Valid @RequestBody SubmitReviewRequest request) {
        Review review = reviewService.submitReview(token, request.rating(), request.comment());

        String googleReviewUrl = null;
        if (review.isFiveStars()) {
            Business business = businessService.findByIdOrThrow(review.getBusinessId());
            googleReviewUrl = business.getGoogleReviewUrl();
        }

        SubmitReviewResponse response = new SubmitReviewResponse(review.getId(), review.getRating(), googleReviewUrl);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Değerlendirmeniz için teşekkür ederiz"));
    }

    @GetMapping("/businesses/{slug}/reviews")
    public ResponseEntity<ApiResponse<List<PublicReviewItemResponse>>> getPublishedReviews(
            @PathVariable String slug) {
        Business business = businessService.findBySlug(slug);
        List<Review> reviews = reviewService.findPublishedForPublicPage(business.getId());

        Map<Long, String> customerNamesById = customerService
            .findAllByIds(reviews.stream().map(Review::getCustomerId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Customer::getId, Customer::getName));

        List<PublicReviewItemResponse> response = reviews.stream()
            .map(review -> ReviewMapper.toPublicItem(review, customerNamesById.get(review.getCustomerId())))
            .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
