package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClient;
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
    private WhatsAppClient whatsAppClient;
    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private BusinessService businessService;

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
        notificationService.sendAppointmentReminder(appointment);

        // Assert
        ArgumentCaptor<WhatsAppMessageRequest> requestCaptor = ArgumentCaptor.forClass(WhatsAppMessageRequest.class);
        verify(whatsAppClient).sendMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().templateName()).isEqualTo("appointment_reminder");
        assertThat(requestCaptor.getValue().to()).isEqualTo("+905551234567");
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
        notificationService.sendAppointmentReminder(appointment);

        // Assert
        verify(whatsAppClient, never()).sendMessage(any(WhatsAppMessageRequest.class));
    }
}
