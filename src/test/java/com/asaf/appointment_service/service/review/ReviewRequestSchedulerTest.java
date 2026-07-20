package com.asaf.appointment_service.service.review;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.notification.NotificationSettings;
import com.asaf.appointment_service.entity.notification.NotificationType;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.service.notification.NotificationSettingsService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRequestSchedulerTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private NotificationSettingsService notificationSettingsService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewRequestScheduler scheduler;

    private Appointment completedAt(LocalDateTime completedAt) {
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .timeSlot(completedAt.minusHours(1), 60)
            .status(AppointmentStatus.COMPLETED)
            .build();
        ReflectionTestUtils.setField(appointment, "id", 100L);
        ReflectionTestUtils.setField(appointment, "completedAt", completedAt);
        return appointment;
    }

    private NotificationSettings enabledSettings(int delayHours) {
        return NotificationSettings.builder().businessId(1L)
            .reviewRequestEnabled(true).reviewRequestDelayHours(delayHours).build();
    }

    @Test
    void should_sendReviewRequest_when_delayHasPassedAndFeatureEnabled() {
        Appointment appointment = completedAt(LocalDateTime.now().minusHours(3));
        when(appointmentRepository.findCompletedBetween(any(), any())).thenReturn(List.of(appointment));
        when(notificationLogRepository.existsByAppointmentIdAndType(100L, NotificationType.REVIEW_REQUEST))
            .thenReturn(false);
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.CUSTOMER_REVIEWS)).thenReturn(true);
        when(notificationSettingsService.getSettings(1L)).thenReturn(enabledSettings(2));

        scheduler.sendScheduledReviewRequests();

        verify(reviewService).requestReview(appointment);
    }

    @Test
    void should_notSend_when_delayHasNotPassedYet() {
        Appointment appointment = completedAt(LocalDateTime.now().minusMinutes(30));
        when(appointmentRepository.findCompletedBetween(any(), any())).thenReturn(List.of(appointment));
        when(notificationLogRepository.existsByAppointmentIdAndType(100L, NotificationType.REVIEW_REQUEST))
            .thenReturn(false);
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.CUSTOMER_REVIEWS)).thenReturn(true);
        when(notificationSettingsService.getSettings(1L)).thenReturn(enabledSettings(2));

        scheduler.sendScheduledReviewRequests();

        verify(reviewService, never()).requestReview(any());
    }

    @Test
    void should_notSend_when_alreadySent() {
        Appointment appointment = completedAt(LocalDateTime.now().minusHours(3));
        when(appointmentRepository.findCompletedBetween(any(), any())).thenReturn(List.of(appointment));
        when(notificationLogRepository.existsByAppointmentIdAndType(100L, NotificationType.REVIEW_REQUEST))
            .thenReturn(true);

        scheduler.sendScheduledReviewRequests();

        verify(reviewService, never()).requestReview(any());
        verify(subscriptionService, never()).isFeatureEnabled(any(), any());
    }

    @Test
    void should_notSend_when_planLacksCustomerReviewsFeature() {
        Appointment appointment = completedAt(LocalDateTime.now().minusHours(3));
        when(appointmentRepository.findCompletedBetween(any(), any())).thenReturn(List.of(appointment));
        when(notificationLogRepository.existsByAppointmentIdAndType(100L, NotificationType.REVIEW_REQUEST))
            .thenReturn(false);
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.CUSTOMER_REVIEWS)).thenReturn(false);

        scheduler.sendScheduledReviewRequests();

        verify(reviewService, never()).requestReview(any());
    }

    @Test
    void should_notSend_when_businessDisabledReviewRequests() {
        Appointment appointment = completedAt(LocalDateTime.now().minusHours(3));
        when(appointmentRepository.findCompletedBetween(any(), any())).thenReturn(List.of(appointment));
        when(notificationLogRepository.existsByAppointmentIdAndType(100L, NotificationType.REVIEW_REQUEST))
            .thenReturn(false);
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.CUSTOMER_REVIEWS)).thenReturn(true);
        NotificationSettings disabled = NotificationSettings.builder().businessId(1L)
            .reviewRequestEnabled(false).build();
        when(notificationSettingsService.getSettings(1L)).thenReturn(disabled);

        scheduler.sendScheduledReviewRequests();

        verify(reviewService, never()).requestReview(any());
    }
}
