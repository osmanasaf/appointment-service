package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.exception.appointment.AppointmentNotFoundException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.customer.PackageService;
import com.asaf.appointment_service.service.customer.PackageSessionService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository repository;
    @Mock private BusinessService businessService;
    @Mock private CustomerService customerService;
    @Mock private EmployeeService employeeService;
    @Mock private ServiceDefinitionService serviceDefinitionService;
    @Mock private PackageService packageService;
    @Mock private PackageSessionService packageSessionService;
    @Mock private AppointmentValidationService validationService;
    @Mock private AppointmentStateService stateService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void confirmAppointment_shouldDelegateToStateService() {
        Long appointmentId = 1L;
        Appointment appointment = buildAppointment(AppointmentStatus.CONFIRMED);
        when(stateService.confirm(appointmentId)).thenReturn(appointment);

        appointmentService.confirmAppointment(appointmentId);

        verify(stateService).confirm(appointmentId);
    }

    @Test
    void cancelAppointment_shouldDelegateToStateService() {
        Long appointmentId = 1L;
        String reason = "Test iptal";
        Appointment appointment = buildAppointment(AppointmentStatus.CANCELLED);
        when(stateService.cancel(appointmentId, reason)).thenReturn(appointment);

        appointmentService.cancelAppointment(appointmentId, reason);

        verify(stateService).cancel(appointmentId, reason);
    }

    @Test
    void completeAppointment_shouldDelegateToStateService() {
        Long appointmentId = 1L;
        Appointment appointment = buildAppointment(AppointmentStatus.COMPLETED);
        when(stateService.complete(appointmentId)).thenReturn(appointment);

        appointmentService.completeAppointment(appointmentId);

        verify(stateService).complete(appointmentId);
    }

    @Test
    void markAsNoShow_shouldDelegateToStateService() {
        Long appointmentId = 1L;
        Appointment appointment = buildAppointment(AppointmentStatus.NO_SHOW);
        when(stateService.markAsNoShow(appointmentId)).thenReturn(appointment);

        appointmentService.markAsNoShow(appointmentId);

        verify(stateService).markAsNoShow(appointmentId);
    }

    @Test
    void markAsRisky_shouldDelegateToStateService() {
        Long appointmentId = 1L;
        String reason = "Onay alınamadı";
        Appointment appointment = buildAppointment(AppointmentStatus.RISKY);
        when(stateService.markAsRisky(appointmentId, reason)).thenReturn(appointment);

        appointmentService.markAsRisky(appointmentId, reason);

        verify(stateService).markAsRisky(appointmentId, reason);
    }

    @Test
    void unassignPackageSession_whenNoSessionLinked_shouldThrowIllegalArgumentException() {
        Long appointmentId = 1L;
        Appointment appointment = buildAppointment(AppointmentStatus.PENDING);

        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.unassignPackageSession(appointmentId))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(packageService);
    }

    @Test
    void findByIdOrThrow_whenNotFound_shouldThrowAppointmentNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.findByIdOrThrow(999L))
            .isInstanceOf(AppointmentNotFoundException.class);
    }

    private Appointment buildAppointment(AppointmentStatus status) {
        return Appointment.builder()
            .businessId(1L).customerId(1L).employeeId(1L).serviceId(1L)
            .scheduledAt(LocalDateTime.now().plusDays(1))
            .endAt(LocalDateTime.now().plusDays(1).plusMinutes(60))
            .durationMinutes(60).status(status).build();
    }
}
