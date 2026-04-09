package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private BusinessService businessService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ReminderScheduler reminderScheduler;

    @Test
    void sendScheduledReminders_shouldFetchBusinessesInBatch() {
        Business business = buildBusiness(1L, 24);
        Appointment a1 = buildAppointment(1L, 1L);
        Appointment a2 = buildAppointment(2L, 1L);
        Appointment a3 = buildAppointment(3L, 1L);

        when(appointmentRepository.findAppointmentsNeedingReminder(any(), any()))
            .thenReturn(List.of(a1, a2, a3));
        when(businessService.findAllByIds(List.of(1L))).thenReturn(List.of(business));
        when(a1.needsReminder(24)).thenReturn(false);

        reminderScheduler.sendScheduledReminders();

        verify(businessService).findAllByIds(List.of(1L));
    }

    @Test
    void sendScheduledReminders_whenNoAppointments_shouldNotQueryBusinesses() {
        when(appointmentRepository.findAppointmentsNeedingReminder(any(), any()))
            .thenReturn(List.of());

        reminderScheduler.sendScheduledReminders();

        verify(businessService, times(0)).findAllByIds(any());
    }

    @Test
    void sendScheduledReminders_shouldSendReminderWhenNeedsReminder() {
        Business business = buildBusiness(1L, 24);
        Appointment appointment = buildAppointment(1L, 1L);

        when(appointmentRepository.findAppointmentsNeedingReminder(any(), any()))
            .thenReturn(List.of(appointment));
        when(businessService.findAllByIds(any())).thenReturn(List.of(business));
        when(appointment.needsReminder(24)).thenReturn(true);
        when(appointmentRepository.save(any())).thenReturn(appointment);

        reminderScheduler.sendScheduledReminders();

        verify(notificationService).sendAppointmentReminder(appointment);
    }

    private Business buildBusiness(Long id, int reminderHours) {
        return Business.builder().id(id).reminderHoursBefore(reminderHours).build();
    }

    private Appointment buildAppointment(Long id, Long businessId) {
        Appointment mock = org.mockito.Mockito.mock(Appointment.class);
        lenient().when(mock.getId()).thenReturn(id);
        when(mock.getBusinessId()).thenReturn(businessId);
        return mock;
    }
}
