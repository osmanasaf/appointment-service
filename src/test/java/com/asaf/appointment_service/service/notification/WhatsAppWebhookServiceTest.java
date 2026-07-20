package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.notification.NotificationLog;
import com.asaf.appointment_service.entity.notification.NotificationType;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageResponse;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private WhatsAppClientInterface whatsAppClient;
    @Mock
    private PublicActionTokenService publicActionTokenService;
    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private WhatsAppWebhookService webhookService;

    @Test
    void should_requestRescheduleAndIssueToken_when_rescheduleKeywordReceivedForConfirmedAppointment() {
        String normalizedPhone = WhatsAppWebhookService.normalizePhone("905551234567");
        Appointment confirmedAppointment = appointmentWithId(100L, 1L, AppointmentStatus.CONFIRMED);
        Appointment rescheduleRequested = appointmentWithId(100L, 1L, AppointmentStatus.RESCHEDULE_REQUESTED);

        when(notificationLogRepository.findTopByPhoneNumberAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(normalizedPhone), anyList(), any(LocalDateTime.class)))
            .thenReturn(Optional.of(logWithAppointmentId(100L)));
        when(appointmentService.findByIdOrThrow(100L)).thenReturn(confirmedAppointment);
        when(appointmentService.requestReschedule(100L, "WhatsApp üzerinden erteleme talebi"))
            .thenReturn(rescheduleRequested);
        when(publicActionTokenService.issueRescheduleToken(1L, 100L))
            .thenReturn(tokenWithValue("tok123"));
        when(whatsAppClient.isEnabled()).thenReturn(true);
        when(emailProperties.baseUrl()).thenReturn("https://randevum.pro");

        webhookService.processWebhookPayload(rescheduleWebhookPayload());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppClient).sendTextMessage(eq(normalizedPhone), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("https://randevum.pro/reschedule/tok123");
    }

    @Test
    void should_reissueTokenWithoutStateTransition_when_appointmentAlreadyRescheduleRequested() {
        String normalizedPhone = WhatsAppWebhookService.normalizePhone("905551234567");
        Appointment alreadyRequested = appointmentWithId(100L, 1L, AppointmentStatus.RESCHEDULE_REQUESTED);

        when(notificationLogRepository.findTopByPhoneNumberAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(normalizedPhone), anyList(), any(LocalDateTime.class)))
            .thenReturn(Optional.of(logWithAppointmentId(100L)));
        when(appointmentService.findByIdOrThrow(100L)).thenReturn(alreadyRequested);
        when(publicActionTokenService.issueRescheduleToken(1L, 100L))
            .thenReturn(tokenWithValue("tok456"));
        when(whatsAppClient.isEnabled()).thenReturn(true);
        when(emailProperties.baseUrl()).thenReturn("https://randevum.pro");

        webhookService.processWebhookPayload(rescheduleWebhookPayload());

        verify(appointmentService, org.mockito.Mockito.never()).requestReschedule(any(), any());
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppClient).sendTextMessage(eq(normalizedPhone), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("tok456");
    }

    @Test
    void should_sendNoAppointmentReply_when_rescheduleKeywordReceivedWithoutMatchingLog() {
        String normalizedPhone = WhatsAppWebhookService.normalizePhone("905551234567");

        when(notificationLogRepository.findTopByPhoneNumberAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(normalizedPhone), anyList(), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());
        when(whatsAppClient.isEnabled()).thenReturn(true);

        webhookService.processWebhookPayload(rescheduleWebhookPayload());

        verify(publicActionTokenService, org.mockito.Mockito.never()).issueRescheduleToken(any(), any());
        verify(whatsAppClient).sendTextMessage(eq(normalizedPhone), any());
    }

    private Map<String, Object> rescheduleWebhookPayload() {
        Map<String, Object> textMap = Map.of("body", "ertele");
        Map<String, Object> msg = Map.of("from", "905551234567", "text", textMap);
        Map<String, Object> value = Map.of("messages", List.of(msg));
        Map<String, Object> change = Map.of("value", value);
        Map<String, Object> entry = Map.of("changes", List.of(change));
        return Map.of("entry", List.of(entry));
    }

    private NotificationLog logWithAppointmentId(Long appointmentId) {
        NotificationLog log = NotificationLog.builder()
            .businessId(1L)
            .appointmentId(appointmentId)
            .type(NotificationType.APPOINTMENT_REMINDER)
            .phoneNumber("905551234567")
            .message("test")
            .build();
        return log;
    }

    private Appointment appointmentWithId(Long id, Long businessId, AppointmentStatus status) {
        Appointment appointment = Appointment.builder()
            .businessId(businessId).customerId(10L).employeeId(2L).serviceId(3L)
            .timeSlot(LocalDateTime.now().plusDays(1), 60)
            .status(status)
            .build();
        ReflectionTestUtils.setField(appointment, "id", id);
        return appointment;
    }

    private PublicActionToken tokenWithValue(String tokenValue) {
        return PublicActionToken.builder()
            .businessId(1L).subjectId(100L).type(PublicActionTokenType.RESCHEDULE)
            .token(tokenValue).expiresAt(Instant.now().plusSeconds(3600)).build();
    }
}
