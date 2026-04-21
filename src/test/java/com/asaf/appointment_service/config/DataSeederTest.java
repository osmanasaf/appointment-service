package com.asaf.appointment_service.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
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
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private CustomerService customerService;
    @Mock
    private SubscriptionService subscriptionService;

    @Test
    void should_assignProPlanAndCreateTwoEmployees_when_seedRunsOnEmptyDatabase() {
        DataSeeder dataSeeder = new DataSeeder(
            userRepository,
            passwordEncoder,
            businessService,
            employeeService,
            serviceDefinitionService,
            customerService,
            subscriptionService
        );

        Business business = Business.builder()
            .id(100L)
            .slug("test-kuafor")
            .build();

        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(businessService.createBusiness(any())).thenReturn(business);

        dataSeeder.run(new DefaultApplicationArguments(new String[0]));

        verify(subscriptionService).initializeTrialSubscription(100L);
        verify(subscriptionService).assignPlanToBusiness(100L, PlanCode.PRO, true);
        verify(employeeService, times(2)).createEmployee(any(), any());
    }
}
