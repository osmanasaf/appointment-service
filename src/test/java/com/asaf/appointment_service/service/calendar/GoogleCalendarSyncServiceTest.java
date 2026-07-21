package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.domain.event.AppointmentCancelled;
import com.asaf.appointment_service.domain.event.AppointmentCreated;
import com.asaf.appointment_service.domain.event.AppointmentRescheduledEvent;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.integration.google.GoogleCalendarClient;
import com.asaf.appointment_service.integration.google.dto.GoogleCalendarEventData;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.calendar.CalendarConnectionRepository;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarSyncServiceTest {

    private static final Long APPOINTMENT_ID = 100L;
    private static final Long EMPLOYEE_ID = 2L;
    private static final Long OTHER_EMPLOYEE_ID = 3L;

    @Mock
    private CalendarConnectionRepository connectionRepository;
    @Mock
    private CalendarConnectionService connectionService;
    @Mock
    private GoogleCalendarClient googleCalendarClient;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private CustomerService customerService;

    private GoogleCalendarSyncService syncService;

    private void setUp() {
        syncService = new GoogleCalendarSyncService(
            connectionRepository, connectionService, googleCalendarClient,
            appointmentRepository, serviceDefinitionService, customerService);
    }

    private Appointment appointment(Long employeeId, String existingGoogleEventId) {
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(employeeId).serviceId(3L)
            .timeSlot(LocalDateTime.now().plusDays(1), 60)
            .status(AppointmentStatus.CONFIRMED)
            .build();
        ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
        if (existingGoogleEventId != null) {
            appointment.linkGoogleEvent(existingGoogleEventId);
        }
        return appointment;
    }

    private CalendarConnection connectedConnection(Long employeeId) {
        return CalendarConnection.builder()
            .businessId(1L).employeeId(employeeId).refreshTokenEncrypted("enc").build();
    }

    @Test
    void should_doNothing_when_noConnectionForEmployee() {
        setUp();
        when(connectionRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

        syncService.onAppointmentCreated(new AppointmentCreated(
            APPOINTMENT_ID, 1L, 10L, EMPLOYEE_ID, 3L, LocalDateTime.now(), LocalDateTime.now().plusHours(1), "CONFIRMED"));

        verify(appointmentRepository, never()).findById(any());
        verify(googleCalendarClient, never()).upsertEvent(any(), any(), any(), any());
    }

    @Test
    void should_upsertEventAndLinkId_when_appointmentCreatedWithActiveConnection() {
        setUp();
        Appointment appointment = appointment(EMPLOYEE_ID, null);
        CalendarConnection connection = connectedConnection(EMPLOYEE_ID);
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).price(BigDecimal.TEN).build();
        Customer customer = Customer.builder().businessId(1L).name("Zeynep").phone("+90", "5551112233").build();

        when(connectionRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(connection));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(connectionService.resolveAccessToken(connection)).thenReturn("access-token");
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(customerService.findByIdOrThrow(10L)).thenReturn(customer);
        when(googleCalendarClient.upsertEvent(eq("access-token"), eq(connection.getGoogleCalendarId()),
                eq(null), any(GoogleCalendarEventData.class)))
            .thenReturn("google-event-1");

        syncService.onAppointmentCreated(new AppointmentCreated(
            APPOINTMENT_ID, 1L, 10L, EMPLOYEE_ID, 3L, LocalDateTime.now(), LocalDateTime.now().plusHours(1), "CONFIRMED"));

        assertThat(appointment.getGoogleEventId()).isEqualTo("google-event-1");
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void should_deleteEventAndUnlink_when_appointmentCancelled() {
        setUp();
        Appointment appointment = appointment(EMPLOYEE_ID, "google-event-1");
        CalendarConnection connection = connectedConnection(EMPLOYEE_ID);

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(connectionRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(connection));
        when(connectionService.resolveAccessToken(connection)).thenReturn("access-token");

        syncService.onAppointmentCancelled(new AppointmentCancelled(
            APPOINTMENT_ID, 1L, 10L, LocalDateTime.now(), "Müşteri iptal etti"));

        verify(googleCalendarClient).deleteEvent("access-token", connection.getGoogleCalendarId(), "google-event-1");
        assertThat(appointment.getGoogleEventId()).isNull();
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void should_deleteFromOldAndCreateInNew_when_rescheduledWithEmployeeChange() {
        setUp();
        Appointment appointment = appointment(OTHER_EMPLOYEE_ID, "old-google-event");
        CalendarConnection oldConnection = connectedConnection(EMPLOYEE_ID);
        CalendarConnection newConnection = connectedConnection(OTHER_EMPLOYEE_ID);
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).price(BigDecimal.TEN).build();
        Customer customer = Customer.builder().businessId(1L).name("Zeynep").phone("+90", "5551112233").build();

        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(connectionRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(oldConnection));
        when(connectionRepository.findByEmployeeId(OTHER_EMPLOYEE_ID)).thenReturn(Optional.of(newConnection));
        when(connectionService.resolveAccessToken(oldConnection)).thenReturn("old-access-token");
        when(connectionService.resolveAccessToken(newConnection)).thenReturn("new-access-token");
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(customerService.findByIdOrThrow(10L)).thenReturn(customer);
        when(googleCalendarClient.upsertEvent(eq("new-access-token"), any(), any(), any()))
            .thenReturn("new-google-event");

        syncService.onAppointmentRescheduled(new AppointmentRescheduledEvent(
            APPOINTMENT_ID, 1L, EMPLOYEE_ID, OTHER_EMPLOYEE_ID, LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(1)));

        verify(googleCalendarClient).deleteEvent("old-access-token", oldConnection.getGoogleCalendarId(), "old-google-event");
        verify(googleCalendarClient).upsertEvent(eq("new-access-token"), any(), any(), any());
    }

    @Test
    void should_notThrow_when_googleClientThrowsException() {
        setUp();
        when(connectionRepository.findByEmployeeId(EMPLOYEE_ID)).thenThrow(new RuntimeException("boom"));

        org.assertj.core.api.Assertions.assertThatCode(() -> syncService.onAppointmentCreated(new AppointmentCreated(
            APPOINTMENT_ID, 1L, 10L, EMPLOYEE_ID, 3L, LocalDateTime.now(), LocalDateTime.now().plusHours(1), "CONFIRMED")))
            .doesNotThrowAnyException();
    }
}
