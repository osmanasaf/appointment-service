package com.asaf.appointment_service.service.review;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.notification.NotificationSettings;
import com.asaf.appointment_service.entity.notification.NotificationType;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.service.notification.NotificationSettingsService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Tamamlanan randevular için, işletmenin ayarladığı gecikme süresi geçtikten
 * sonra tek seferlik WhatsApp değerlendirme isteği gönderir.
 */
@Service
public class ReviewRequestScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewRequestScheduler.class);
    private static final int LOOKBACK_DAYS = 7;

    private final AppointmentRepository appointmentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final SubscriptionService subscriptionService;
    private final ReviewService reviewService;

    public ReviewRequestScheduler(AppointmentRepository appointmentRepository,
                                  NotificationLogRepository notificationLogRepository,
                                  NotificationSettingsService notificationSettingsService,
                                  SubscriptionService subscriptionService,
                                  ReviewService reviewService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationSettingsService = notificationSettingsService;
        this.subscriptionService = subscriptionService;
        this.reviewService = reviewService;
    }

    @Scheduled(fixedDelayString = "${review.request.scheduler.interval:3600000}")
    public void sendScheduledReviewRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> completed = appointmentRepository.findCompletedBetween(now.minusDays(LOOKBACK_DAYS), now);

        int sentCount = 0;
        for (Appointment appointment : completed) {
            try {
                if (shouldSendReviewRequest(appointment, now)) {
                    reviewService.requestReview(appointment);
                    sentCount++;
                }
            } catch (Exception e) {
                log.error("Review isteği gönderilemedi: appointmentId={}, error={}",
                    appointment.getId(), e.getMessage());
            }
        }
        log.info("Review request scheduler tamamlandı: {} istek gönderildi", sentCount);
    }

    private boolean shouldSendReviewRequest(Appointment appointment, LocalDateTime now) {
        if (appointment.getCompletedAt() == null) {
            return false;
        }
        if (notificationLogRepository.existsByAppointmentIdAndType(
                appointment.getId(), NotificationType.REVIEW_REQUEST)) {
            return false;
        }
        if (!subscriptionService.isFeatureEnabled(appointment.getBusinessId(), PlanFeature.CUSTOMER_REVIEWS)) {
            return false;
        }
        NotificationSettings settings = notificationSettingsService.getSettings(appointment.getBusinessId());
        if (!settings.isReviewRequestEnabled()) {
            return false;
        }
        LocalDateTime dueAt = appointment.getCompletedAt().plusHours(settings.getReviewRequestDelayHours());
        return !now.isBefore(dueAt);
    }
}
