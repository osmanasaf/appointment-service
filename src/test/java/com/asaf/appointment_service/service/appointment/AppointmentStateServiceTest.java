package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.exception.appointment.AppointmentNotFoundException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.customer.PackageService;
import com.asaf.appointment_service.service.customer.PackageSessionService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentStateServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PackageSessionService packageSessionService;
    @Mock private PackageService packageService;
    @Mock private CustomerService customerService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentStateService stateService;

    @Test
    void confirm_whenPending_shouldSetConfirmedStatus() {
        Appointment appointment = buildAppointment(AppointmentStatus.PENDING);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = stateService.confirm(1L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(result.getConfirmedAt()).isNotNull();
    }

    @Test
    void cancel_whenPackageSessionLinked_shouldUnassignSession() {
        Appointment appointment = buildAppointment(AppointmentStatus.PENDING);
        appointment.setPackageSessionId(42L);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        stateService.cancel(1L, "iptal");

        verify(packageSessionService).unassignAppointment(42L);
        assertThat(appointment.getPackageSessionId()).isNull();
    }

    @Test
    void cancel_whenNoPackageSession_shouldNotCallUnassign() {
        Appointment appointment = buildAppointment(AppointmentStatus.PENDING);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        stateService.cancel(1L, "iptal");

        verify(packageSessionService, never()).unassignAppointment(any());
    }

    @Test
    void complete_whenPackageSessionLinked_shouldMarkCompleted() {
        Appointment appointment = buildAppointment(AppointmentStatus.CONFIRMED);
        appointment.setPackageSessionId(7L);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        stateService.complete(1L);

        verify(packageSessionService).markCompleted(7L);
    }

    @Test
    void markAsNoShow_shouldBurnSessionAndRecordNoShow() {
        Appointment appointment = buildAppointment(AppointmentStatus.CONFIRMED);
        appointment.setPackageSessionId(99L);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = stateService.markAsNoShow(1L);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        verify(packageSessionService).markNoShow(99L);
        verify(customerService).recordNoShow(appointment.getCustomerId());
    }

    @Test
    void markAsRisky_shouldSetRiskyFlag() {
        Appointment appointment = buildAppointment(AppointmentStatus.PENDING);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = stateService.markAsRisky(1L, "geç onay");

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.RISKY);
        assertThat(result.isRisky()).isTrue();
    }

    @Test
    void findByIdOrThrow_whenNotFound_shouldThrow() {
        when(appointmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stateService.findByIdOrThrow(404L))
            .isInstanceOf(AppointmentNotFoundException.class);
    }

    private Appointment buildAppointment(AppointmentStatus status) {
        return Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .scheduledAt(LocalDateTime.now().plusHours(1))
            .endAt(LocalDateTime.now().plusHours(2))
            .durationMinutes(60).status(status).build();
    }
}
