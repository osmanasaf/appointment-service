package com.asaf.appointment_service.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BusinessService businessService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private EmployeeCapabilityService employeeCapabilityService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private CustomerService customerService;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private SubscriptionService subscriptionService;

    @Test
    void should_seedDemoBusinessData_when_seedRunsOnEmptyDatabase() {
        DataSeeder dataSeeder = new DataSeeder(
            userRepository,
            passwordEncoder,
            businessService,
            employeeService,
            employeeCapabilityService,
            serviceDefinitionService,
            customerService,
            appointmentService,
            subscriptionService
        );

        Business business = Business.builder()
            .id(100L)
            .slug("test-kuafor")
            .build();

        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(businessService.createBusiness(any())).thenReturn(business);
        when(employeeService.createEmployee(any(), any())).thenReturn(
            employee(201L), employee(202L), employee(203L), employee(204L));
        when(serviceDefinitionService.createService(any(), any())).thenReturn(
            service(301L), service(302L), service(303L), service(304L), service(305L));
        when(customerService.createCustomer(any(), any())).thenReturn(
            customer(401L), customer(402L), customer(403L), customer(404L), customer(405L), customer(406L));

        dataSeeder.run(new DefaultApplicationArguments(new String[0]));

        verify(subscriptionService).initializeTrialSubscription(100L);
        verify(subscriptionService).assignPlanToBusiness(100L, PlanCode.PRO, true);
        verify(employeeService, times(4)).createEmployee(any(), any());
        verify(serviceDefinitionService, times(5)).createService(any(), any());
        verify(customerService, times(6)).createCustomer(any(), any());
        verify(employeeCapabilityService, times(10)).assignCapability(any(), any());
        verify(appointmentService, times(6)).createAppointment(any(), any());
    }

    private Employee employee(Long id) {
        return Employee.builder().id(id).build();
    }

    private ServiceDefinition service(Long id) {
        return ServiceDefinition.builder().id(id).build();
    }

    private Customer customer(Long id) {
        return Customer.builder().id(id).build();
    }
}
