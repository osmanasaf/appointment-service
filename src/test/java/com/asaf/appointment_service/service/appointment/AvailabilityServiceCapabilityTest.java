package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.dto.appointment.AvailableSlotResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.exception.employee.EmployeeCapabilityNotFoundException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.business.BusinessWorkingHoursRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceCapabilityTest {

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

    private static final Long BUSINESS_ID = 1L;
    private static final Long EMPLOYEE_ID = 5L;
    private static final Long SERVICE_ID = 10L;

    @BeforeEach
    void setUp() {
        // Çalışana özel takvim tanımlı değil → işletme saatleri kullanılır
        lenient().when(scheduleService.getScheduleForDay(anyLong(), any(DayOfWeek.class)))
            .thenReturn(Optional.empty());
    }

    @Test
    void getAvailableSlots_whenEmployeeHasCapability_shouldReturnSlots() {
        LocalDate date = LocalDate.now().plusDays(1);
        Business business = buildBusiness();
        Employee employee = Employee.builder().businessId(BUSINESS_ID).name("Ali").build();
        ServiceDefinition service = buildService(60);
        BusinessWorkingHours hours = BusinessWorkingHours.defaultHours(BUSINESS_ID, date.getDayOfWeek());

        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        when(capabilityService.effectiveDuration(EMPLOYEE_ID, SERVICE_ID, 60)).thenReturn(60);
        when(workingHoursRepository.findByBusinessIdAndDayOfWeek(BUSINESS_ID, date.getDayOfWeek()))
            .thenReturn(Optional.of(hours));
        when(appointmentRepository.findConflictingAppointments(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());

        List<AvailableSlotResponse> slots =
            availabilityService.getAvailableSlots(BUSINESS_ID, EMPLOYEE_ID, SERVICE_ID, date);

        assertThat(slots).isNotEmpty();
    }

    @Test
    void getAvailableSlots_whenEmployeeHasNoCapability_shouldThrow() {
        LocalDate date = LocalDate.now().plusDays(1);
        Business business = buildBusiness();
        Employee employee = Employee.builder().businessId(BUSINESS_ID).name("Ali").build();
        ServiceDefinition service = buildService(60);

        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        doThrow(new EmployeeCapabilityNotFoundException("Ali", "Test Service"))
            .when(capabilityService).validateCapability(EMPLOYEE_ID, SERVICE_ID);

        assertThatThrownBy(() ->
            availabilityService.getAvailableSlots(BUSINESS_ID, EMPLOYEE_ID, SERVICE_ID, date))
            .isInstanceOf(EmployeeCapabilityNotFoundException.class);
    }

    @Test
    void getAvailableSlots_whenNoEmployeeId_shouldAggregateCapableEmployees() {
        LocalDate date = LocalDate.now().plusDays(1);
        Business business = buildBusiness();
        ServiceDefinition service = buildService(60);
        BusinessWorkingHours hours = BusinessWorkingHours.defaultHours(BUSINESS_ID, date.getDayOfWeek());

        EmployeeServiceCapability cap = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).build();

        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);
        when(capabilityService.getActiveByServiceAndBusiness(SERVICE_ID, BUSINESS_ID))
            .thenReturn(List.of(cap));
        when(workingHoursRepository.findByBusinessIdAndDayOfWeek(BUSINESS_ID, date.getDayOfWeek()))
            .thenReturn(Optional.of(hours));
        when(appointmentRepository.findConflictingAppointments(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());

        List<AvailableSlotResponse> slots =
            availabilityService.getAvailableSlots(BUSINESS_ID, null, SERVICE_ID, date);

        assertThat(slots).isNotEmpty();
    }

    @Test
    void getAvailableSlots_whenNoEmployeeIdAndNoCapableEmployees_shouldReturnEmpty() {
        LocalDate date = LocalDate.now().plusDays(1);
        Business business = buildBusiness();
        ServiceDefinition service = buildService(60);

        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);
        when(capabilityService.getActiveByServiceAndBusiness(SERVICE_ID, BUSINESS_ID))
            .thenReturn(List.of());

        List<AvailableSlotResponse> slots =
            availabilityService.getAvailableSlots(BUSINESS_ID, null, SERVICE_ID, date);

        assertThat(slots).isEmpty();
    }

    @Test
    void getAvailableSlots_whenPastDate_shouldThrow() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Business business = buildBusiness();
        ServiceDefinition service = buildService(30);

        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);

        assertThatThrownBy(() ->
            availabilityService.getAvailableSlots(BUSINESS_ID, EMPLOYEE_ID, SERVICE_ID, yesterday))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Geçmiş");
    }

    @Test
    void getAvailableSlots_whenCapableEmployeeUsesCustomDuration_shouldRespectCustomDuration() {
        LocalDate date = LocalDate.now().plusDays(1);
        Business business = buildBusiness();
        ServiceDefinition service = buildService(60);
        BusinessWorkingHours hours = BusinessWorkingHours.defaultHours(BUSINESS_ID, date.getDayOfWeek());

        EmployeeServiceCapability cap = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).customDurationMinutes(30).build();

        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);
        when(capabilityService.getActiveByServiceAndBusiness(SERVICE_ID, BUSINESS_ID))
            .thenReturn(List.of(cap));
        when(workingHoursRepository.findByBusinessIdAndDayOfWeek(BUSINESS_ID, date.getDayOfWeek()))
            .thenReturn(Optional.of(hours));
        when(appointmentRepository.findConflictingAppointments(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());

        List<AvailableSlotResponse> slotsWithCustom30 =
            availabilityService.getAvailableSlots(BUSINESS_ID, null, SERVICE_ID, date);

        EmployeeServiceCapability capDefault = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).build();
        when(capabilityService.getActiveByServiceAndBusiness(SERVICE_ID, BUSINESS_ID))
            .thenReturn(List.of(capDefault));

        List<AvailableSlotResponse> slotsWithDefault60 =
            availabilityService.getAvailableSlots(BUSINESS_ID, null, SERVICE_ID, date);

        assertThat(slotsWithCustom30.size()).isGreaterThan(slotsWithDefault60.size());
    }

    private Business buildBusiness() {
        return Business.builder()
            .name("Test Salon").slug("test-salon")
            .category(BusinessCategory.OTHER)
            .sameDayBookingAllowed(true)
            .maxAdvanceBookingDays(30)
            .build();
    }

    private ServiceDefinition buildService(int durationMinutes) {
        return ServiceDefinition.builder()
            .businessId(BUSINESS_ID)
            .name("Test Hizmet")
            .durationMinutes(durationMinutes)
            .buffers(0, 0)
            .build();
    }
}
