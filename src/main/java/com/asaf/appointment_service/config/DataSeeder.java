package com.asaf.appointment_service.config;

import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.dto.customer.CreateCustomerRequest;
import com.asaf.appointment_service.dto.employee.AssignCapabilityRequest;
import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.dto.service.CreateServiceRequest;
import com.asaf.appointment_service.entity.appointment.AppointmentSource;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.SkillLevel;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.repository.user.UserRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
 * Only runs when no users exist. Creates one business with realistic demo employees, services, customers and appointments.
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
    private final EmployeeCapabilityService employeeCapabilityService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final CustomerService customerService;
    private final AppointmentService appointmentService;
    private final SubscriptionService subscriptionService;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      BusinessService businessService,
                      EmployeeService employeeService,
                      EmployeeCapabilityService employeeCapabilityService,
                      ServiceDefinitionService serviceDefinitionService,
                      CustomerService customerService,
                      AppointmentService appointmentService,
                      SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.businessService = businessService;
        this.employeeService = employeeService;
        this.employeeCapabilityService = employeeCapabilityService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.customerService = customerService;
        this.appointmentService = appointmentService;
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
                null,
                BusinessCategory.HAIR_SALON
        ));
        subscriptionService.initializeTrialSubscription(business.getId());
        subscriptionService.assignPlanToBusiness(business.getId(), PlanCode.PRO, true);

        User user = User.builder()
                .email(SEED_EMAIL)
                .password(passwordEncoder.encode(SEED_PASSWORD))
                .name("Admin Kullanıcı")
                .phoneNumber("5551234567")
                .businessId(business.getId())
                .role(UserRole.BUSINESS_OWNER)
                .build();
        user.verifyEmail();
        userRepository.save(user);

        Employee ayse = employeeService.createEmployee(business.getId(), new CreateEmployeeRequest(
                "Ayşe Çalışan",
                "5559876543",
                "+90",
                "ayse@test.com",
                "Kısa saç kesimi ve fön konusunda uzman.",
                "Kuaför",
                true
        ));
        Employee fatma = employeeService.createEmployee(business.getId(), new CreateEmployeeRequest(
                "Fatma Çalışan",
                "5559876500",
                "+90",
                "fatma@test.com",
                "Renk, bakım ve saç tasarımı hizmetlerinde deneyimli.",
                "Saç Tasarım Uzmanı",
                false
        ));
        Employee mehmet = employeeService.createEmployee(business.getId(), new CreateEmployeeRequest(
                "Mehmet Usta",
                "5559876511",
                "+90",
                "mehmet@test.com",
                "Sakal tasarımı ve erkek bakımında uzman.",
                "Berber",
                false
        ));
        Employee zeynep = employeeService.createEmployee(business.getId(), new CreateEmployeeRequest(
                "Zeynep Bakım",
                "5559876522",
                "+90",
                "zeynep@test.com",
                "Cilt bakımı, kaş ve makyaj uygulamaları yapar.",
                "Güzellik Uzmanı",
                false
        ));

        ServiceDefinition haircut = serviceDefinitionService.createService(business.getId(), new CreateServiceRequest(
                "Saç Kesimi",
                "Klasik saç kesimi",
                30,
                0,
                0,
                BigDecimal.valueOf(150),
                false,
                true,
                true
        ));
        ServiceDefinition blowDry = serviceDefinitionService.createService(business.getId(), new CreateServiceRequest(
                "Fön",
                "Günlük veya özel gün fön uygulaması",
                30,
                0,
                0,
                BigDecimal.valueOf(120),
                false,
                true,
                true
        ));
        ServiceDefinition coloring = serviceDefinitionService.createService(business.getId(), new CreateServiceRequest(
                "Saç Boyama",
                "Tek renk saç boyama ve bakım",
                90,
                10,
                10,
                BigDecimal.valueOf(650),
                true,
                false,
                false
        ));
        ServiceDefinition beardTrim = serviceDefinitionService.createService(business.getId(), new CreateServiceRequest(
                "Sakal Tıraşı",
                "Sakal kesimi ve şekillendirme",
                20,
                0,
                0,
                BigDecimal.valueOf(100),
                false,
                true,
                true
        ));
        ServiceDefinition skinCare = serviceDefinitionService.createService(business.getId(), new CreateServiceRequest(
                "Cilt Bakımı",
                "Temel cilt temizliği ve nemlendirme bakımı",
                60,
                5,
                5,
                BigDecimal.valueOf(450),
                true,
                false,
                false
        ));

        assignCapabilities(ayse, haircut, blowDry, coloring);
        assignCapabilities(fatma, haircut, blowDry, coloring);
        assignCapabilities(mehmet, haircut, beardTrim);
        assignCapabilities(zeynep, skinCare, blowDry);

        Customer demo = customerService.createCustomer(business.getId(), new CreateCustomerRequest(
                "Demo Müşteri",
                "5551112233",
                "+90",
                "musteri@test.com",
                "Demo kayıt, test akışları için kullanılabilir.",
                "Moda, Kadıköy / İstanbul",
                null
        ));
        Customer elif = customerService.createCustomer(business.getId(), new CreateCustomerRequest(
                "Elif Yılmaz",
                "5551112244",
                "+90",
                "elif.yilmaz@test.com",
                "Fön için genelde öğleden sonra gelir.",
                "Ataşehir / İstanbul",
                null
        ));
        Customer murat = customerService.createCustomer(business.getId(), new CreateCustomerRequest(
                "Murat Kaya",
                "5551112255",
                "+90",
                "murat.kaya@test.com",
                "Sakal çizgisi hassas, Mehmet Usta'yı tercih eder.",
                "Üsküdar / İstanbul",
                null
        ));
        Customer deniz = customerService.createCustomer(business.getId(), new CreateCustomerRequest(
                "Deniz Acar",
                "5551112266",
                "+90",
                "deniz.acar@test.com",
                "Saç boyama için alerji testi hatırlat.",
                "Beşiktaş / İstanbul",
                null
        ));
        Customer selin = customerService.createCustomer(business.getId(), new CreateCustomerRequest(
                "Selin Demir",
                "5551112277",
                "+90",
                "selin.demir@test.com",
                "Cilt bakımı paketleriyle ilgileniyor.",
                "Şişli / İstanbul",
                null
        ));
        Customer can = customerService.createCustomer(business.getId(), new CreateCustomerRequest(
                "Can Özkan",
                "5551112288",
                "+90",
                "can.ozkan@test.com",
                null,
                "Bakırköy / İstanbul",
                null
        ));

        LocalDateTime baseTime = nextWeekdayAt(10, 0);
        appointmentService.createAppointment(business.getId(), new CreateAppointmentRequest(
                demo.getId(), ayse.getId(), haircut.getId(), baseTime, "İlk demo randevu", null, AppointmentSource.PHONE));
        appointmentService.createAppointment(business.getId(), new CreateAppointmentRequest(
                elif.getId(), fatma.getId(), blowDry.getId(), baseTime.plusHours(1), "Özel gün hazırlığı", null, AppointmentSource.ONLINE));
        appointmentService.createAppointment(business.getId(), new CreateAppointmentRequest(
                murat.getId(), mehmet.getId(), beardTrim.getId(), baseTime.plusHours(2), "Sakal çizgisi özenli yapılacak", null, AppointmentSource.WALK_IN));
        appointmentService.createAppointment(business.getId(), new CreateAppointmentRequest(
                deniz.getId(), fatma.getId(), coloring.getId(), baseTime.plusDays(1).plusHours(1), "Alerji testi sorulacak", null, AppointmentSource.PHONE));
        appointmentService.createAppointment(business.getId(), new CreateAppointmentRequest(
                selin.getId(), zeynep.getId(), skinCare.getId(), baseTime.plusDays(1).plusHours(3), "Hassas cilt notu var", null, AppointmentSource.ONLINE));
        appointmentService.createAppointment(business.getId(), new CreateAppointmentRequest(
                can.getId(), ayse.getId(), blowDry.getId(), baseTime.plusDays(2).plusHours(2), null, "Hızlı fön talebi", AppointmentSource.ONLINE));

        log.info("Seed completed. Login: {} / {}", SEED_EMAIL, SEED_PASSWORD);
        log.info("Public randevu linki: /b/{}", business.getSlug());
    }

    private void assignCapabilities(Employee employee, ServiceDefinition... services) {
        for (ServiceDefinition service : services) {
            employeeCapabilityService.assignCapability(employee.getId(), new AssignCapabilityRequest(
                    service.getId(),
                    null,
                    null,
                    SkillLevel.SENIOR
            ));
        }
    }

    private LocalDateTime nextWeekdayAt(int hour, int minute) {
        LocalDateTime candidate = LocalDateTime.now().plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        while (candidate.getDayOfWeek().getValue() > 5) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
