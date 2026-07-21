package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.dto.appointment.RescheduleContextResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.notification.NotificationService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescheduleServiceTest {

    @Mock
    private PublicActionTokenService tokenService;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentValidationService validationService;
    @Mock
    private BusinessService businessService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RescheduleService rescheduleService;

    @Test
    void should_returnContext_when_getContextCalledWithValidToken() {
        PublicActionToken token = tokenFor(100L);
        Appointment appointment = appointmentWithStatus(AppointmentStatus.RESCHEDULE_REQUESTED, 100L);
        Business business = Business.builder().id(1L).slug("kuafor-x").name("Kuaför X").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .id(3L).businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0).build();
        Employee employee = Employee.builder().id(2L).businessId(1L).name("Ayşe").build();

        when(tokenService.validate("tok", PublicActionTokenType.RESCHEDULE)).thenReturn(token);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(businessService.findByIdOrThrow(1L)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(employeeService.findByIdOrThrow(2L)).thenReturn(employee);

        RescheduleContextResponse result = rescheduleService.getContext("tok");

        assertThat(result.businessSlug()).isEqualTo("kuafor-x");
        assertThat(result.serviceName()).isEqualTo("Saç Kesimi");
        assertThat(result.employeeName()).isEqualTo("Ayşe");
        assertThat(result.status()).isEqualTo(AppointmentStatus.RESCHEDULE_REQUESTED);
    }

    @Test
    void should_confirmAppointmentAndConsumeToken_when_rescheduleCalledAndAutoConfirmEligible() {
        PublicActionToken token = tokenFor(100L);
        Appointment appointment = appointmentWithStatus(AppointmentStatus.RESCHEDULE_REQUESTED, 100L);
        Long originalEmployeeId = appointment.getEmployeeId();
        LocalDateTime originalScheduledAt = appointment.getScheduledAt();
        Business business = Business.builder().id(1L).slug("kuafor-x").autoConfirm(true).build();
        ServiceDefinition service = ServiceDefinition.builder()
            .id(3L).businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0)
            .requiresConfirmation(false).build();
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);

        when(tokenService.validate("tok", PublicActionTokenType.RESCHEDULE)).thenReturn(token);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(businessService.findByIdOrThrow(1L)).thenReturn(business);
        when(employeeService.findByIdOrThrow(5L))
            .thenReturn(Employee.builder().id(5L).businessId(1L).name("Yeni Çalışan").build());
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = rescheduleService.reschedule("tok", 5L, newStart);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(result.getEmployeeId()).isEqualTo(5L);
        assertThat(result.getScheduledAt()).isEqualTo(newStart);
        verify(validationService).validateSlotAvailabilityExcluding(1L, 5L, newStart, newStart.plusMinutes(60), 100L);
        verify(validationService).validateDayLimitExcluding(business, appointment.getCustomerId(), newStart, 100L);
        verify(tokenService).consume(token);
        verify(notificationService).sendRescheduleConfirmation(result);

        org.mockito.ArgumentCaptor<Object> eventCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        com.asaf.appointment_service.domain.event.SlotFreedEvent slotFreedEvent = eventCaptor.getAllValues().stream()
            .filter(com.asaf.appointment_service.domain.event.SlotFreedEvent.class::isInstance)
            .map(com.asaf.appointment_service.domain.event.SlotFreedEvent.class::cast)
            .findFirst().orElseThrow();
        assertThat(slotFreedEvent.employeeId()).isEqualTo(originalEmployeeId);
        assertThat(slotFreedEvent.scheduledAt()).isEqualTo(originalScheduledAt);

        com.asaf.appointment_service.domain.event.AppointmentRescheduledEvent rescheduledEvent =
            eventCaptor.getAllValues().stream()
                .filter(com.asaf.appointment_service.domain.event.AppointmentRescheduledEvent.class::isInstance)
                .map(com.asaf.appointment_service.domain.event.AppointmentRescheduledEvent.class::cast)
                .findFirst().orElseThrow();
        assertThat(rescheduledEvent.oldEmployeeId()).isEqualTo(originalEmployeeId);
        assertThat(rescheduledEvent.newEmployeeId()).isEqualTo(5L);
        assertThat(rescheduledEvent.newScheduledAt()).isEqualTo(newStart);
    }

    @Test
    void should_movePending_when_rescheduleCalledAndServiceRequiresConfirmation() {
        PublicActionToken token = tokenFor(100L);
        Appointment appointment = appointmentWithStatus(AppointmentStatus.RESCHEDULE_REQUESTED, 100L);
        Business business = Business.builder().id(1L).slug("kuafor-x").autoConfirm(true).build();
        ServiceDefinition service = ServiceDefinition.builder()
            .id(3L).businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0)
            .requiresConfirmation(true).build();
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);

        when(tokenService.validate("tok", PublicActionTokenType.RESCHEDULE)).thenReturn(token);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(businessService.findByIdOrThrow(1L)).thenReturn(business);
        when(employeeService.findByIdOrThrow(2L)).thenReturn(Employee.builder().id(2L).businessId(1L).name("Ayşe").build());
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = rescheduleService.reschedule("tok", 2L, newStart);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void should_throwBusinessException_when_appointmentNotInRescheduleRequestedStatus() {
        PublicActionToken token = tokenFor(100L);
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CONFIRMED, 100L);

        when(tokenService.validate("tok", PublicActionTokenType.RESCHEDULE)).thenReturn(token);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> rescheduleService.reschedule("tok", 5L, LocalDateTime.now().plusDays(1)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ertelenebilir");

        verify(tokenService, never()).consume(any());
        verify(appointmentRepository, never()).save(any());
    }

    private PublicActionToken tokenFor(Long appointmentId) {
        return PublicActionToken.builder()
            .businessId(1L).subjectId(appointmentId).type(PublicActionTokenType.RESCHEDULE)
            .token("tok").expiresAt(Instant.now().plusSeconds(3600)).build();
    }

    private Appointment appointmentWithStatus(AppointmentStatus status, Long id) {
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .timeSlot(LocalDateTime.now().plusDays(1), 60)
            .status(status)
            .build();
        ReflectionTestUtils.setField(appointment, "id", id);
        return appointment;
    }
}
