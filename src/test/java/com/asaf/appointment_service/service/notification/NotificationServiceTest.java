package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.integration.sms.SmsClient;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageRequest;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageResponse;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private WhatsAppClientInterface whatsAppClient;
    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private NotificationSettingsService notificationSettingsService;
    @Mock
    private CustomerService customerService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private BusinessService businessService;
    @Mock
    private SmsClient smsClient;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendAppointmentReminder_whenNotAlreadySent_shouldCallWhatsAppAndSaveLog() {
        // Arrange
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .scheduledAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(1).plusMinutes(60))
            .durationMinutes(60).status(AppointmentStatus.CONFIRMED).build();
        Customer customer = Customer.builder().businessId(1L).name("Ali").phone("+90", "5551234567").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0).build();

        when(notificationLogRepository.existsByAppointmentIdAndType(appointment.getId(),
            com.asaf.appointment_service.entity.notification.NotificationType.APPOINTMENT_REMINDER)).thenReturn(false);
        when(customerService.findByIdOrThrow(10L)).thenReturn(customer);
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(whatsAppClient.sendMessage(any(WhatsAppMessageRequest.class)))
            .thenReturn(WhatsAppMessageResponse.success("msg-1"));

        // Act
        notificationService.sendAppointmentReminder(appointment, "https://randevum.pro/api/v1/public/appointments/tok/calendar.ics");

        // Assert
        ArgumentCaptor<WhatsAppMessageRequest> requestCaptor = ArgumentCaptor.forClass(WhatsAppMessageRequest.class);
        verify(whatsAppClient).sendMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().templateName()).isEqualTo("appointment_reminder");
        assertThat(requestCaptor.getValue().to()).isEqualTo("+905551234567");
        assertThat(requestCaptor.getValue().parameters()).containsEntry(
            "calendar_link", "https://randevum.pro/api/v1/public/appointments/tok/calendar.ics");
        verify(notificationLogRepository).save(any());
    }

    @Test
    void sendWaitlistSlotOffer_shouldSendMessageAndLogWithoutAppointmentId() {
        Customer customer = Customer.builder().businessId(1L).name("Ali").phone("+90", "5551234567").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0).build();

        when(whatsAppClient.sendMessage(any(WhatsAppMessageRequest.class)))
            .thenReturn(WhatsAppMessageResponse.success("msg-1"));

        notificationService.sendWaitlistSlotOffer(1L, customer, service,
            LocalDateTime.now().plusDays(1), "https://randevum.pro/waitlist-offer/tok123");

        ArgumentCaptor<WhatsAppMessageRequest> requestCaptor = ArgumentCaptor.forClass(WhatsAppMessageRequest.class);
        verify(whatsAppClient).sendMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().templateName()).isEqualTo("waitlist_slot_offer");
        assertThat(requestCaptor.getValue().parameters()).containsEntry(
            "offer_link", "https://randevum.pro/waitlist-offer/tok123");
        verify(notificationLogRepository).save(any());
    }

    @Test
    void sendReviewRequest_shouldSendMessageWithReviewLink() {
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .scheduledAt(LocalDateTime.now().minusDays(1)).endAt(LocalDateTime.now().minusDays(1).plusMinutes(60))
            .durationMinutes(60).status(AppointmentStatus.COMPLETED).build();
        Customer customer = Customer.builder().businessId(1L).name("Ali").phone("+90", "5551234567").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0).build();

        when(customerService.findByIdOrThrow(10L)).thenReturn(customer);
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(whatsAppClient.sendMessage(any(WhatsAppMessageRequest.class)))
            .thenReturn(WhatsAppMessageResponse.success("msg-1"));

        notificationService.sendReviewRequest(appointment, "https://randevum.pro/review/tok123");

        ArgumentCaptor<WhatsAppMessageRequest> requestCaptor = ArgumentCaptor.forClass(WhatsAppMessageRequest.class);
        verify(whatsAppClient).sendMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().templateName()).isEqualTo("review_request");
        assertThat(requestCaptor.getValue().parameters()).containsEntry(
            "review_link", "https://randevum.pro/review/tok123");
        verify(notificationLogRepository).save(any());
    }

    @Test
    void sendAppointmentReminder_whenAlreadySent_shouldNotCallWhatsApp() {
        // Arrange
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .scheduledAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(1).plusMinutes(60))
            .durationMinutes(60).status(AppointmentStatus.CONFIRMED).build();

        when(notificationLogRepository.existsByAppointmentIdAndType(appointment.getId(),
            com.asaf.appointment_service.entity.notification.NotificationType.APPOINTMENT_REMINDER)).thenReturn(true);

        // Act (businessService not called when reminder already sent)
        notificationService.sendAppointmentReminder(appointment, "https://randevum.pro/calendar.ics");

        // Assert
        verify(whatsAppClient, never()).sendMessage(any(WhatsAppMessageRequest.class));
    }
}
