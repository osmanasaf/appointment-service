package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.dto.employee.AssignCapabilityRequest;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import com.asaf.appointment_service.entity.employee.SkillLevel;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.exception.employee.EmployeeCapabilityNotFoundException;
import com.asaf.appointment_service.repository.employee.EmployeeCapabilityRepository;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.math.BigDecimal;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeCapabilityServiceTest {

    @Mock
    private EmployeeCapabilityRepository capabilityRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;

    @InjectMocks
    private EmployeeCapabilityService capabilityService;

    private static final Long EMPLOYEE_ID = 1L;
    private static final Long SERVICE_ID = 10L;

    @Test
    void assignCapability_whenNoExistingCapability_shouldCreateNew() {
        Employee employee = Employee.builder().businessId(1L).name("Ali").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).build();
        AssignCapabilityRequest request = new AssignCapabilityRequest(SERVICE_ID, 45, null, SkillLevel.SENIOR);

        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);
        when(capabilityRepository.findByEmployeeIdAndServiceId(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(Optional.empty());
        when(capabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeServiceCapability result = capabilityService.assignCapability(EMPLOYEE_ID, request);

        assertThat(result.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(result.getServiceId()).isEqualTo(SERVICE_ID);
        assertThat(result.getSkillLevel()).isEqualTo(SkillLevel.SENIOR);
        assertThat(result.getCustomDurationMinutes()).isEqualTo(45);
        assertThat(result.isActive()).isTrue();
        verify(capabilityRepository).save(any(EmployeeServiceCapability.class));
    }

    @Test
    void assignCapability_whenCapabilityExistsButInactive_shouldReactivate() {
        Employee employee = Employee.builder().businessId(1L).name("Ali").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).build();
        AssignCapabilityRequest request = new AssignCapabilityRequest(SERVICE_ID, null, BigDecimal.valueOf(200), null);

        EmployeeServiceCapability existing = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).build();
        existing.deactivate();

        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);
        when(capabilityRepository.findByEmployeeIdAndServiceId(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(Optional.of(existing));
        when(capabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeServiceCapability result = capabilityService.assignCapability(EMPLOYEE_ID, request);

        assertThat(result.isActive()).isTrue();
        assertThat(result.getCustomPrice()).isEqualTo(BigDecimal.valueOf(200));
        verify(capabilityRepository).save(existing);
    }

    @Test
    void removeCapability_whenCapabilityExists_shouldDeactivate() {
        EmployeeServiceCapability capability = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).build();

        when(capabilityRepository.findByEmployeeIdAndServiceId(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(Optional.of(capability));
        when(capabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        capabilityService.removeCapability(EMPLOYEE_ID, SERVICE_ID);

        assertThat(capability.isActive()).isFalse();
        verify(capabilityRepository).save(capability);
    }

    @Test
    void removeCapability_whenCapabilityNotFound_shouldThrow() {
        Employee employee = Employee.builder().businessId(1L).name("Ali").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).build();

        when(capabilityRepository.findByEmployeeIdAndServiceId(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(Optional.empty());
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);

        assertThatThrownBy(() -> capabilityService.removeCapability(EMPLOYEE_ID, SERVICE_ID))
            .isInstanceOf(EmployeeCapabilityNotFoundException.class);

        verify(capabilityRepository, never()).save(any());
    }

    @Test
    void validateCapability_whenCapabilityActive_shouldNotThrow() {
        when(capabilityRepository.existsByEmployeeIdAndServiceIdAndActiveTrue(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(true);

        capabilityService.validateCapability(EMPLOYEE_ID, SERVICE_ID);
    }

    @Test
    void validateCapability_whenCapabilityMissing_shouldThrow() {
        Employee employee = Employee.builder().businessId(1L).name("Ali").build();
        ServiceDefinition service = ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).build();

        when(capabilityRepository.existsByEmployeeIdAndServiceIdAndActiveTrue(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(false);
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        when(serviceDefinitionService.findByIdOrThrow(SERVICE_ID)).thenReturn(service);

        assertThatThrownBy(() -> capabilityService.validateCapability(EMPLOYEE_ID, SERVICE_ID))
            .isInstanceOf(EmployeeCapabilityNotFoundException.class);
    }

    @Test
    void getByEmployee_shouldReturnOnlyActiveCapabilities() {
        Employee employee = Employee.builder().businessId(1L).name("Ali").build();
        EmployeeServiceCapability cap = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).build();

        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        when(capabilityRepository.findByEmployeeIdAndActiveTrue(EMPLOYEE_ID)).thenReturn(List.of(cap));

        List<EmployeeServiceCapability> result = capabilityService.getByEmployee(EMPLOYEE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getServiceId()).isEqualTo(SERVICE_ID);
    }

    @Test
    void effectiveDuration_whenCustomDurationSet_shouldReturnCustom() {
        EmployeeServiceCapability cap = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).customDurationMinutes(45).build();

        when(capabilityRepository.findByEmployeeIdAndServiceId(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(Optional.of(cap));

        int result = capabilityService.effectiveDuration(EMPLOYEE_ID, SERVICE_ID, 60);

        assertThat(result).isEqualTo(45);
    }

    @Test
    void effectiveDuration_whenNoCustomDuration_shouldReturnServiceDefault() {
        EmployeeServiceCapability cap = EmployeeServiceCapability.builder()
            .employeeId(EMPLOYEE_ID).serviceId(SERVICE_ID).build();

        when(capabilityRepository.findByEmployeeIdAndServiceId(EMPLOYEE_ID, SERVICE_ID))
            .thenReturn(Optional.of(cap));

        int result = capabilityService.effectiveDuration(EMPLOYEE_ID, SERVICE_ID, 60);

        assertThat(result).isEqualTo(60);
    }
}
