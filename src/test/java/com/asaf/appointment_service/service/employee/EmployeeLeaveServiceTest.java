package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.domain.valueobject.LeavePeriod;
import com.asaf.appointment_service.dto.employee.CreateLeaveRequest;
import com.asaf.appointment_service.dto.employee.LeaveConflictAction;
import com.asaf.appointment_service.dto.employee.LeaveConflictPreviewResponse;
import com.asaf.appointment_service.dto.employee.LeaveResponse;
import com.asaf.appointment_service.dto.employee.ResolveLeaveConflictRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeLeave;
import com.asaf.appointment_service.entity.employee.LeaveStatus;
import com.asaf.appointment_service.entity.employee.LeaveType;
import com.asaf.appointment_service.exception.employee.EmployeeLeaveNotFoundException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.employee.EmployeeLeaveRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeLeaveServiceTest {

    private static final Long BUSINESS_ID = 10L;
    private static final Long EMPLOYEE_ID = 1L;
    private static final Long NEW_EMPLOYEE_ID = 2L;
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate NEXT_WEEK = TODAY.plusWeeks(1);

    @Mock private EmployeeLeaveRepository leaveRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentService appointmentService;
    @Mock private EmployeeService employeeService;
    @Mock private EmployeeCapabilityService capabilityService;

    @InjectMocks
    private EmployeeLeaveService leaveService;

    @BeforeEach
    void stubPrimaryEmployee() {
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(
            Employee.builder()
                .id(EMPLOYEE_ID)
                .businessId(BUSINESS_ID)
                .name("Primary")
                .build());
    }

    @Test
    void should_create_leave_when_no_conflicts() {
        CreateLeaveRequest request = new CreateLeaveRequest(TODAY, NEXT_WEEK, null, null, LeaveType.ANNUAL, null);
        EmployeeLeave savedLeave = buildLeave(TODAY, NEXT_WEEK);
        when(leaveRepository.save(any())).thenReturn(savedLeave);

        LeaveResponse response = leaveService.createLeave(EMPLOYEE_ID, request);

        assertThat(response).isNotNull();
        verify(leaveRepository).save(any(EmployeeLeave.class));
    }

    @Test
    void should_throw_when_start_date_in_past() {
        CreateLeaveRequest request = new CreateLeaveRequest(
            TODAY.minusDays(1), NEXT_WEEK, null, null, LeaveType.ANNUAL, null);

        assertThatThrownBy(() -> leaveService.createLeave(EMPLOYEE_ID, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("geçmişte olamaz");
    }

    @Test
    void should_return_conflict_preview_with_appointments() {
        CreateLeaveRequest request = new CreateLeaveRequest(TODAY, NEXT_WEEK, null, null, LeaveType.ANNUAL, null);
        Appointment appointment = buildAppointment();
        when(appointmentRepository.findConflictingAppointmentsForBusiness(
            eq(BUSINESS_ID), eq(EMPLOYEE_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(appointment));

        LeaveConflictPreviewResponse preview = leaveService.previewConflicts(EMPLOYEE_ID, request);

        assertThat(preview.hasConflicts()).isTrue();
        assertThat(preview.totalCount()).isEqualTo(1);
        assertThat(preview.conflictingAppointments()).hasSize(1);
    }

    @Test
    void should_return_empty_conflict_preview_when_no_conflicts() {
        CreateLeaveRequest request = new CreateLeaveRequest(TODAY, NEXT_WEEK, null, null, LeaveType.ANNUAL, null);
        when(appointmentRepository.findConflictingAppointmentsForBusiness(
            eq(BUSINESS_ID), eq(EMPLOYEE_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        LeaveConflictPreviewResponse preview = leaveService.previewConflicts(EMPLOYEE_ID, request);

        assertThat(preview.hasConflicts()).isFalse();
        assertThat(preview.totalCount()).isZero();
    }

    @Test
    void should_cancel_all_conflicts_and_create_leave() {
        CreateLeaveRequest leaveRequest = new CreateLeaveRequest(TODAY, NEXT_WEEK, null, null, LeaveType.SICK, null);
        ResolveLeaveConflictRequest resolveRequest = new ResolveLeaveConflictRequest(LeaveConflictAction.CANCEL_ALL, null);
        Appointment appointment = buildAppointment();
        EmployeeLeave savedLeave = buildLeave(TODAY, NEXT_WEEK);

        when(appointmentRepository.findConflictingAppointmentsForBusiness(
            eq(BUSINESS_ID), eq(EMPLOYEE_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(appointment));
        when(leaveRepository.save(any())).thenReturn(savedLeave);

        leaveService.resolveConflictsAndCreateLeave(EMPLOYEE_ID, leaveRequest, resolveRequest);

        verify(appointmentService).cancelAppointment(eq(appointment.getId()), any());
        verify(leaveRepository).save(any(EmployeeLeave.class));
    }

    @Test
    void should_reassign_all_conflicts_and_create_leave() {
        CreateLeaveRequest leaveRequest = new CreateLeaveRequest(TODAY, NEXT_WEEK, null, null, LeaveType.UNPAID, null);
        ResolveLeaveConflictRequest resolveRequest = new ResolveLeaveConflictRequest(LeaveConflictAction.REASSIGN_ALL, NEW_EMPLOYEE_ID);
        Appointment appointment = buildAppointment();
        EmployeeLeave savedLeave = buildLeave(TODAY, NEXT_WEEK);

        when(employeeService.findByIdOrThrow(NEW_EMPLOYEE_ID)).thenReturn(
            Employee.builder()
                .id(NEW_EMPLOYEE_ID)
                .businessId(BUSINESS_ID)
                .name("Replacement")
                .build());

        when(appointmentRepository.findConflictingAppointmentsForBusiness(
            eq(BUSINESS_ID), eq(EMPLOYEE_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(appointment));
        when(leaveRepository.save(any())).thenReturn(savedLeave);

        leaveService.resolveConflictsAndCreateLeave(EMPLOYEE_ID, leaveRequest, resolveRequest);

        verify(capabilityService).validateCapability(NEW_EMPLOYEE_ID, appointment.getServiceId());
        verify(appointmentService).reassignAppointmentEmployee(appointment.getId(), NEW_EMPLOYEE_ID);
        verify(leaveRepository).save(any(EmployeeLeave.class));
    }

    @Test
    void should_throw_when_cancelling_nonexistent_leave() {
        when(leaveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.cancelLeave(EMPLOYEE_ID, 99L))
            .isInstanceOf(EmployeeLeaveNotFoundException.class);
    }

    @Test
    void should_throw_when_reassign_missing_new_employee_id() {
        CreateLeaveRequest leaveRequest = new CreateLeaveRequest(TODAY, NEXT_WEEK, null, null, LeaveType.ANNUAL, null);
        ResolveLeaveConflictRequest resolveRequest = new ResolveLeaveConflictRequest(LeaveConflictAction.REASSIGN_ALL, null);

        assertThatThrownBy(() ->
            leaveService.resolveConflictsAndCreateLeave(EMPLOYEE_ID, leaveRequest, resolveRequest))
            .isInstanceOf(NullPointerException.class);

        verify(leaveRepository, never()).save(any());
    }

    private EmployeeLeave buildLeave(LocalDate start, LocalDate end) {
        return EmployeeLeave.builder()
            .employeeId(EMPLOYEE_ID)
            .period(LeavePeriod.fullDay(start, end))
            .leaveType(LeaveType.ANNUAL)
            .build();
    }

    private Appointment buildAppointment() {
        LocalDateTime start = LocalDateTime.of(TODAY.plusDays(2), java.time.LocalTime.of(10, 0));
        return Appointment.builder()
            .businessId(10L)
            .customerId(20L)
            .employeeId(EMPLOYEE_ID)
            .serviceId(30L)
            .timeSlot(start, 60)
            .source(com.asaf.appointment_service.entity.appointment.AppointmentSource.ONLINE)
            .build();
    }
}
