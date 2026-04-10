package com.asaf.appointment_service.config;

import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Uygulama başlangıcında plan verilerini oluşturur.
 * Planlar yoksa seed eder, varsa atlar.
 */
@Component
@Order(0) // DataSeeder'dan önce çalışsın
public class PlanSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanSeeder.class);

    private final PlanRepository planRepository;

    public PlanSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            log.debug("Plan seed skipped: plans already exist");
            return;
        }

        log.info("Seeding subscription plans...");

        // STARTER - Ücretsiz demo paketi
        Plan starter = Plan.builder()
                .code(PlanCode.STARTER)
                .maxEmployees(1)
                .maxAppointmentsMonthly(20)
                .maxServices(3)
                .maxBranches(1)
                .maxWhatsappMonthly(0)
                .maxSmsMonthly(0)
                .features(List.of(
                        PlanFeature.ONLINE_BOOKING
                ))
                .monthlyPrice(BigDecimal.ZERO)
                .yearlyPrice(BigDecimal.ZERO)
                .build();
        planRepository.save(starter);

        // PRO - Küçük işletmeler için
        Plan pro = Plan.builder()
                .code(PlanCode.PRO)
                .maxEmployees(5)
                .maxAppointmentsMonthly(500)
                .maxServices(20)
                .maxBranches(1)
                .maxWhatsappMonthly(500)
                .maxSmsMonthly(0)
                .features(List.of(
                        PlanFeature.ONLINE_BOOKING,
                        PlanFeature.WHATSAPP_NOTIFICATIONS,
                        PlanFeature.PACKAGE_MANAGEMENT,
                        PlanFeature.NO_SHOW_TRACKING,
                        PlanFeature.CUSTOMER_NOTES,
                        PlanFeature.BASIC_REPORTS
                ))
                .monthlyPrice(new BigDecimal("590.00"))
                .yearlyPrice(new BigDecimal("5900.00"))
                .build();
        planRepository.save(pro);

        // BUSINESS - Ana hedef paket
        Plan business = Plan.builder()
                .code(PlanCode.BUSINESS)
                .maxEmployees(10)
                .maxAppointmentsMonthly(-1) // Sınırsız
                .maxServices(50)
                .maxBranches(1)
                .maxWhatsappMonthly(2000)
                .maxSmsMonthly(100)
                .features(List.of(
                        PlanFeature.ONLINE_BOOKING,
                        PlanFeature.REMOVE_BRANDING,
                        PlanFeature.WHATSAPP_NOTIFICATIONS,
                        PlanFeature.SMS_NOTIFICATIONS,
                        PlanFeature.PACKAGE_MANAGEMENT,
                        PlanFeature.NO_SHOW_TRACKING,
                        PlanFeature.CUSTOMER_NOTES,
                        PlanFeature.BASIC_REPORTS,
                        PlanFeature.ADVANCED_REPORTS,
                        PlanFeature.EXPORT_REPORTS,
                        PlanFeature.PRIORITY_SUPPORT
                ))
                .monthlyPrice(new BigDecimal("1090.00"))
                .yearlyPrice(new BigDecimal("10900.00"))
                .build();
        planRepository.save(business);

        // PREMIUM - Büyük salonlar ve zincirler için
        Plan premium = Plan.builder()
                .code(PlanCode.PREMIUM)
                .maxEmployees(-1) // Sınırsız
                .maxAppointmentsMonthly(-1) // Sınırsız
                .maxServices(-1) // Sınırsız
                .maxBranches(1)
                .maxWhatsappMonthly(5000)
                .maxSmsMonthly(500)
                .features(List.of(
                        PlanFeature.ONLINE_BOOKING,
                        PlanFeature.REMOVE_BRANDING,
                        PlanFeature.CUSTOM_DOMAIN,
                        PlanFeature.WHATSAPP_NOTIFICATIONS,
                        PlanFeature.SMS_NOTIFICATIONS,
                        PlanFeature.PACKAGE_MANAGEMENT,
                        PlanFeature.NO_SHOW_TRACKING,
                        PlanFeature.CUSTOMER_NOTES,
                        PlanFeature.BASIC_REPORTS,
                        PlanFeature.ADVANCED_REPORTS,
                        PlanFeature.EXPORT_REPORTS,
                        PlanFeature.PRIORITY_SUPPORT,
                        PlanFeature.SUPPORT_24_7,
                        PlanFeature.API_ACCESS
                ))
                .monthlyPrice(new BigDecimal("1990.00"))
                .yearlyPrice(new BigDecimal("19900.00"))
                .build();
        planRepository.save(premium);

        log.info("Plan seed completed: STARTER, PRO, BUSINESS, PREMIUM");
    }
}
