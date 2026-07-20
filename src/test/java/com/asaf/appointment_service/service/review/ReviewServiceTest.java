package com.asaf.appointment_service.service.review;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.dto.review.ReviewContextResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.review.Review;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository repository;
    @Mock
    private PublicActionTokenService tokenService;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private BusinessService businessService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private ReviewService reviewService;

    private Appointment completedAppointment(Long id) {
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .timeSlot(LocalDateTime.now().minusDays(1), 60)
            .status(AppointmentStatus.COMPLETED)
            .build();
        ReflectionTestUtils.setField(appointment, "id", id);
        return appointment;
    }

    private PublicActionToken reviewToken(Long appointmentId) {
        return PublicActionToken.builder()
            .businessId(1L).subjectId(appointmentId).type(PublicActionTokenType.REVIEW)
            .token("tok").expiresAt(Instant.now().plusSeconds(3600)).build();
    }

    @Test
    void should_issueTokenAndSendNotification_when_requestReviewCalled() {
        Appointment appointment = completedAppointment(100L);
        when(tokenService.issueReviewToken(1L, 100L)).thenReturn(reviewToken(100L));
        when(emailProperties.baseUrl()).thenReturn("https://randevum.pro");

        reviewService.requestReview(appointment);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendReviewRequest(org.mockito.ArgumentMatchers.eq(appointment), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).isEqualTo("https://randevum.pro/review/tok");
    }

    @Test
    void should_returnContext_when_getContextCalledWithValidToken() {
        PublicActionToken token = reviewToken(100L);
        Appointment appointment = completedAppointment(100L);
        Business business = Business.builder().id(1L).slug("kuafor-x").name("Kuaför X").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .id(3L).businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0).build();

        when(tokenService.validate("tok", PublicActionTokenType.REVIEW)).thenReturn(token);
        when(appointmentService.findByIdOrThrow(100L)).thenReturn(appointment);
        when(businessService.findByIdOrThrow(1L)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(repository.existsByAppointmentId(100L)).thenReturn(false);

        ReviewContextResponse context = reviewService.getContext("tok");

        assertThat(context.businessName()).isEqualTo("Kuaför X");
        assertThat(context.serviceName()).isEqualTo("Saç Kesimi");
        assertThat(context.alreadySubmitted()).isFalse();
    }

    @Test
    void should_saveSanitizedReviewAndConsumeToken_when_submitReviewCalled() {
        PublicActionToken token = reviewToken(100L);
        Appointment appointment = completedAppointment(100L);

        when(tokenService.validate("tok", PublicActionTokenType.REVIEW)).thenReturn(token);
        when(repository.existsByAppointmentId(100L)).thenReturn(false);
        when(appointmentService.findByIdOrThrow(100L)).thenReturn(appointment);
        when(repository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.submitReview("tok", 5, "<b>Harika</b> bir deneyimdi");

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Harika bir deneyimdi");
        assertThat(result.getBusinessId()).isEqualTo(1L);
        assertThat(result.getCustomerId()).isEqualTo(10L);
        verify(tokenService).consume(token);
    }

    @Test
    void should_throwBusinessException_when_submitReviewCalledTwiceForSameAppointment() {
        PublicActionToken token = reviewToken(100L);
        when(tokenService.validate("tok", PublicActionTokenType.REVIEW)).thenReturn(token);
        when(repository.existsByAppointmentId(100L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.submitReview("tok", 4, "Güzel"))
            .isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
        verify(tokenService, never()).consume(any());
    }

    @Test
    void should_publishReview_when_reviewBelongsToBusiness() {
        Review review = Review.builder().businessId(1L).appointmentId(100L).customerId(10L).rating(5).build();
        when(repository.findById(50L)).thenReturn(Optional.of(review));

        reviewService.publish(1L, 50L);

        assertThat(review.getStatus().name()).isEqualTo("PUBLISHED");
        verify(repository).save(review);
    }

    @Test
    void should_throwReviewNotFound_when_reviewBelongsToDifferentBusiness() {
        Review review = Review.builder().businessId(2L).appointmentId(100L).customerId(10L).rating(5).build();
        when(repository.findById(50L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.publish(1L, 50L))
            .isInstanceOf(ReviewNotFoundException.class);
    }
}
