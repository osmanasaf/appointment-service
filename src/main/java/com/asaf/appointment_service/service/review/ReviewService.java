package com.asaf.appointment_service.service.review;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.common.util.TextSanitizer;
import com.asaf.appointment_service.dto.review.ReviewContextResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.review.Review;
import com.asaf.appointment_service.entity.review.ReviewStatus;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.exception.review.ReviewNotFoundException;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.repository.review.ReviewRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.notification.NotificationService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final int MAX_COMMENT_LENGTH = 1000;
    private static final int PUBLIC_REVIEW_LIMIT = 5;

    private final ReviewRepository repository;
    private final PublicActionTokenService tokenService;
    private final AppointmentService appointmentService;
    private final BusinessService businessService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final NotificationService notificationService;
    private final EmailProperties emailProperties;

    public ReviewService(ReviewRepository repository,
                         PublicActionTokenService tokenService,
                         AppointmentService appointmentService,
                         BusinessService businessService,
                         ServiceDefinitionService serviceDefinitionService,
                         NotificationService notificationService,
                         EmailProperties emailProperties) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.appointmentService = appointmentService;
        this.businessService = businessService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.notificationService = notificationService;
        this.emailProperties = emailProperties;
    }

    /** ReviewRequestScheduler tarafından, tamamlanan randevu için gecikmeli olarak çağrılır. */
    @Transactional
    public void requestReview(Appointment appointment) {
        PublicActionToken token = tokenService.issueReviewToken(appointment.getBusinessId(), appointment.getId());
        String reviewLink = emailProperties.baseUrl() + "/review/" + token.getToken();
        notificationService.sendReviewRequest(appointment, reviewLink);
    }

    @Transactional(readOnly = true)
    public ReviewContextResponse getContext(String token) {
        PublicActionToken actionToken = tokenService.validate(token, PublicActionTokenType.REVIEW);
        Appointment appointment = appointmentService.findByIdOrThrow(actionToken.getSubjectId());
        Business business = businessService.findByIdOrThrow(appointment.getBusinessId());
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(appointment.getServiceId());

        return new ReviewContextResponse(
            appointment.getId(),
            business.getName(),
            service.getName(),
            appointment.getScheduledAt(),
            repository.existsByAppointmentId(appointment.getId())
        );
    }

    @Transactional
    public Review submitReview(String token, int rating, String comment) {
        PublicActionToken actionToken = tokenService.validate(token, PublicActionTokenType.REVIEW);
        Long appointmentId = actionToken.getSubjectId();

        if (repository.existsByAppointmentId(appointmentId)) {
            throw new BusinessException("REVIEW_ALREADY_SUBMITTED",
                "Bu randevu için zaten bir değerlendirme yapılmış.");
        }

        Appointment appointment = appointmentService.findByIdOrThrow(appointmentId);
        Review review = Review.builder()
            .businessId(appointment.getBusinessId())
            .appointmentId(appointmentId)
            .customerId(appointment.getCustomerId())
            .employeeId(appointment.getEmployeeId())
            .rating(rating)
            .comment(TextSanitizer.stripHtml(comment, MAX_COMMENT_LENGTH))
            .build();
        Review saved = repository.save(review);

        tokenService.consume(actionToken);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Review> listByBusiness(Long businessId, ReviewStatus statusFilter) {
        if (statusFilter != null) {
            return repository.findByBusinessIdAndStatusOrderByCreatedAtDesc(
                businessId, statusFilter, PageRequest.of(0, Integer.MAX_VALUE));
        }
        return repository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    @Transactional
    public void publish(Long businessId, Long reviewId) {
        Review review = findByIdForBusinessOrThrow(businessId, reviewId);
        review.publish();
        repository.save(review);
    }

    @Transactional
    public void reject(Long businessId, Long reviewId) {
        Review review = findByIdForBusinessOrThrow(businessId, reviewId);
        review.reject();
        repository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> findPublishedForPublicPage(Long businessId) {
        return repository.findByBusinessIdAndStatusOrderByCreatedAtDesc(
            businessId, ReviewStatus.PUBLISHED, PageRequest.of(0, PUBLIC_REVIEW_LIMIT));
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long businessId) {
        return repository.findAverageRating(businessId);
    }

    @Transactional(readOnly = true)
    public long getPublishedCount(Long businessId) {
        return repository.countByBusinessIdAndStatus(businessId, ReviewStatus.PUBLISHED);
    }

    private Review findByIdForBusinessOrThrow(Long businessId, Long reviewId) {
        Review review = repository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!review.getBusinessId().equals(businessId)) {
            throw new ReviewNotFoundException(reviewId);
        }
        return review;
    }
}
