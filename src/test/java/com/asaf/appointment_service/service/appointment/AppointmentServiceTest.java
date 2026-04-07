package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.exception.appointment.AppointmentNotFoundException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.customer.PackageService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository repository;
    @Mock
    private BusinessService businessService;
    @Mock
    private CustomerService customerService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private PackageService packageService;
    @Mock
    private CustomerPackageRepository customerPackageRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void confirmAppointment_whenPendingAppointment_shouldChangeStatusToConfirmed() {
        // Arrange
        Long appointmentId = 1L;
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(1L).employeeId(1L).serviceId(1L)
            .scheduledAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(1).plusMinutes(60))
            .durationMinutes(60).status(AppointmentStatus.PENDING).build();

        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Appointment result = appointmentService.confirmAppointment(appointmentId);

        // Assert
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(result.getConfirmedAt()).isNotNull();
        verify(repository).save(appointment);
    }

    @Test
    void confirmAppointment_whenNotFound_shouldThrowAppointmentNotFoundException() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.confirmAppointment(999L))
            .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void cancelAppointment_whenPendingAppointment_shouldChangeStatusToCancelled() {
        // Arrange
        Long appointmentId = 1L;
        String reason = "Müşteri iptal etti";
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(1L).employeeId(1L).serviceId(1L)
            .scheduledAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(1).plusMinutes(60))
            .durationMinutes(60).status(AppointmentStatus.PENDING).build();

        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Appointment result = appointmentService.cancelAppointment(appointmentId, reason);

        // Assert
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo(reason);
        verify(repository).save(appointment);
    }

    @Test
    void completeAppointment_whenConfirmedAppointment_shouldChangeStatusToCompleted() {
        // Arrange
        Long appointmentId = 1L;
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(1L).employeeId(1L).serviceId(1L)
            .scheduledAt(LocalDateTime.now().minusHours(1)).endAt(LocalDateTime.now())
            .durationMinutes(60).status(AppointmentStatus.CONFIRMED).build();

        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Appointment result = appointmentService.completeAppointment(appointmentId);

        // Assert
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(repository).save(appointment);
    }

    @Test
    void markAsRisky_whenPendingAppointment_shouldSetRiskyAndReason() {
        // Arrange
        Long appointmentId = 1L;
        String reason = "Onay alınamadı";
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(1L).employeeId(1L).serviceId(1L)
            .scheduledAt(LocalDateTime.now().plusDays(1)).endAt(LocalDateTime.now().plusDays(1).plusMinutes(60))
            .durationMinutes(60).status(AppointmentStatus.PENDING).build();

        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Appointment result = appointmentService.markAsRisky(appointmentId, reason);

        // Assert
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.RISKY);
        assertThat(result.isRisky()).isTrue();
        assertThat(result.getRiskReason()).isEqualTo(reason);
        verify(repository).save(appointment);
    }
}
