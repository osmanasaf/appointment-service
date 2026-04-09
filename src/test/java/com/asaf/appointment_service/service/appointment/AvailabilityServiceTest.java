package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.dto.appointment.AvailableSlotResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.business.BusinessWorkingHoursRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private BusinessService businessService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private EmployeeCapabilityService capabilityService;
    @Mock
    private EmployeeScheduleService scheduleService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private BusinessWorkingHoursRepository workingHoursRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void getAvailableSlots_whenWorkingHoursAndNoAppointments_shouldReturnSlots() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        Long businessId = 1L;
        Long employeeId = 1L;
        Long serviceId = 1L;

        Business business = Business.builder().name("Test").slug("test")
            .category(BusinessCategory.OTHER)
            .sameDayBookingAllowed(true).maxAdvanceBookingDays(30).build();
        Employee employee = Employee.builder().businessId(businessId).name("Test").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(businessId).name("Saç").durationMinutes(60).buffers(0, 0).build();
        BusinessWorkingHours hours = BusinessWorkingHours.defaultHours(businessId, date.getDayOfWeek());

        when(businessService.findByIdOrThrow(businessId)).thenReturn(business);
        when(employeeService.findByIdOrThrow(employeeId)).thenReturn(employee);
        when(serviceDefinitionService.findByIdOrThrow(serviceId)).thenReturn(service);
        when(capabilityService.effectiveDuration(employeeId, serviceId, 60)).thenReturn(60);
        when(workingHoursRepository.findByBusinessIdAndDayOfWeek(businessId, date.getDayOfWeek()))
            .thenReturn(Optional.of(hours));
        when(appointmentRepository.findActiveByEmployeeAndDateRange(
                eq(employeeId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());

        // Act
        List<AvailableSlotResponse> slots = availabilityService.getAvailableSlots(businessId, employeeId, serviceId, date);

        // Assert
        assertThat(slots).isNotEmpty();
        assertThat(slots.get(0).startTime().toLocalTime()).isAfterOrEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(0).endTime().toLocalTime()).isBeforeOrEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void getAvailableSlots_whenSlotHasConflictingAppointment_shouldExcludeSlot() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        Long businessId = 1L;
        Long employeeId = 1L;
        Long serviceId = 1L;
        LocalDateTime conflictStart = LocalDateTime.of(date, LocalTime.of(10, 0));
        LocalDateTime conflictEnd = LocalDateTime.of(date, LocalTime.of(11, 0));

        Business business = Business.builder().name("Test").slug("test")
            .category(BusinessCategory.OTHER)
            .sameDayBookingAllowed(true).maxAdvanceBookingDays(30).build();
        Employee employee = Employee.builder().businessId(businessId).name("Test").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(businessId).name("Saç").durationMinutes(60).buffers(0, 0).build();
        BusinessWorkingHours hours = BusinessWorkingHours.defaultHours(businessId, date.getDayOfWeek());
        Appointment conflicting = Appointment.builder()
            .businessId(businessId).customerId(1L).employeeId(employeeId).serviceId(serviceId)
            .scheduledAt(conflictStart).endAt(conflictEnd).durationMinutes(60).build();

        when(businessService.findByIdOrThrow(businessId)).thenReturn(business);
        when(employeeService.findByIdOrThrow(employeeId)).thenReturn(employee);
        when(serviceDefinitionService.findByIdOrThrow(serviceId)).thenReturn(service);
        when(capabilityService.effectiveDuration(employeeId, serviceId, 60)).thenReturn(60);
        when(workingHoursRepository.findByBusinessIdAndDayOfWeek(businessId, date.getDayOfWeek()))
            .thenReturn(Optional.of(hours));
        when(appointmentRepository.findActiveByEmployeeAndDateRange(
                eq(employeeId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(conflicting));

        // Act
        List<AvailableSlotResponse> slots = availabilityService.getAvailableSlots(businessId, employeeId, serviceId, date);

        // Assert
        boolean hasTenOClockSlot = slots.stream()
            .anyMatch(s -> s.startTime().getHour() == 10 && s.startTime().getMinute() == 0);
        assertThat(hasTenOClockSlot).isFalse();
    }
}
