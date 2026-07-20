package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeStatus;
import com.asaf.appointment_service.exception.employee.EmployeeNotFoundException;
import com.asaf.appointment_service.repository.employee.EmployeeRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository repository;
    @Mock private BusinessService businessService;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void findByIdOrThrow_whenNotFound_shouldThrowEmployeeNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findByIdOrThrow(99L))
            .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void activateEmployee_shouldSetStatusToActive() {
        Employee employee = buildEmployee(EmployeeStatus.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee result = employeeService.activateEmployee(1L);

        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void deactivateEmployee_shouldSetStatusToInactive() {
        Employee employee = buildEmployee(EmployeeStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee result = employeeService.deactivateEmployee(1L);

        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
    }

    @Test
    void updatePublicVisibility_shouldToggleShowOnPublicPage() {
        Employee employee = buildEmployee(EmployeeStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee result = employeeService.updatePublicVisibility(1L, false);

        assertThat(result.isShowOnPublicPage()).isFalse();
    }

    private Employee buildEmployee(EmployeeStatus status) {
        return Employee.builder()
            .id(1L).businessId(10L).name("Test Çalışan").status(status).build();
    }
}
