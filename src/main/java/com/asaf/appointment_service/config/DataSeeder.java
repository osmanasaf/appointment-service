package com.asaf.appointment_service.config;

import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.dto.customer.CreateCustomerRequest;
import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.dto.service.CreateServiceRequest;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds initial data when the database is empty (e.g. H2 in-memory on first run).
 * Only runs when no users exist. Creates one business, one admin user, one employee, one service, one customer.
 */
@Component
@Order(1)
@Profile("!prod")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String SEED_EMAIL = "admin@test.com";
    private static final String SEED_PASSWORD = "admin123";
    private static final String SEED_BUSINESS_NAME = "Test Kuaför";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessService businessService;
    private final EmployeeService employeeService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final CustomerService customerService;
    private final SubscriptionService subscriptionService;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      BusinessService businessService,
                      EmployeeService employeeService,
                      ServiceDefinitionService serviceDefinitionService,
                      CustomerService customerService,
                      SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.businessService = businessService;
        this.employeeService = employeeService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.customerService = customerService;
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.debug("Seed skipped: users already exist");
            return;
        }

        log.info("Seeding initial data…");

        var business = businessService.createBusiness(new CreateBusinessRequest(
                SEED_BUSINESS_NAME,
                "5551234567",
                "+90",
                SEED_EMAIL,
                null,
                null
        ));
        subscriptionService.initializeTrialSubscription(business.getId());

        User user = User.builder()
                .email(SEED_EMAIL)
                .password(passwordEncoder.encode(SEED_PASSWORD))
                .name("Admin Kullanıcı")
                .phoneNumber("5551234567")
                .businessId(business.getId())
                .role(UserRole.BUSINESS_OWNER)
                .build();
        userRepository.save(user);

        employeeService.createEmployee(new CreateEmployeeRequest(
                business.getId(),
                "Ayşe Çalışan",
                "5559876543",
                "+90",
                "ayse@test.com",
                "Kuaför",
                true
        ));

        serviceDefinitionService.createService(new CreateServiceRequest(
                business.getId(),
                "Saç Kesimi",
                "Klasik saç kesimi",
                30,
                0,
                0,
                java.math.BigDecimal.valueOf(150),
                false,
                true,
                true
        ));

        customerService.createCustomer(new CreateCustomerRequest(
                business.getId(),
                "Demo Müşteri",
                "5551112233",
                "+90",
                "musteri@test.com",
                null,
                null,
                null
        ));

        log.info("Seed completed. Login: {} / {}", SEED_EMAIL, SEED_PASSWORD);
        log.info("Public randevu linki: /b/{}", business.getSlug());
    }
}
