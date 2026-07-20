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
 * Uygulama baslangicinda abonelik planlarini olusturur.
 * Yeni kayitlar SOLO planina TRIAL status ile 14 gun acilir.
 * Trial bittiginde isletme aktif odemeli plana gecmek zorunda.
 */
@Component
@Order(0)
public class PlanSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanSeeder.class);

    private final PlanRepository planRepository;

    public PlanSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    private static final List<String> CUSTOMER_ACQUISITION_FEATURES = List.of(
            PlanFeature.WAITLIST,
            PlanFeature.CUSTOMER_REVIEWS,
            PlanFeature.DEPOSIT_PAYMENTS,
            PlanFeature.CALENDAR_SYNC,
            PlanFeature.MINI_WEBSITE
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (planRepository.count() == 0) {
            log.info("Seeding subscription plans...");
            planRepository.save(buildSoloPlan());
            planRepository.save(buildProPlan());
            planRepository.save(buildEnterprisePlan());
            log.info("Plan seed completed: SOLO, PRO, ENTERPRISE");
            return;
        }

        syncMissingFeatures(PlanCode.PRO, CUSTOMER_ACQUISITION_FEATURES);
        syncMissingFeatures(PlanCode.ENTERPRISE, CUSTOMER_ACQUISITION_FEATURES);
    }

    /**
     * Var olan plan kayıtlarına, uygulama güncellemesiyle eklenen yeni feature
     * kodlarını sonradan işler. Zaten mevcut olanları tekrar eklemez.
     */
    private void syncMissingFeatures(PlanCode code, List<String> featureCodes) {
        planRepository.findByCode(code).ifPresent(plan -> {
            if (plan.mergeFeatures(featureCodes)) {
                planRepository.save(plan);
                log.info("Plan feature sync: {} plan updated with new features", code);
            }
        });
    }

    private Plan buildSoloPlan() {
        return Plan.builder()
                .code(PlanCode.SOLO)
                .maxEmployees(1)
                .maxAppointmentsMonthly(200)
                .maxServices(10)
                .maxBranches(1)
                .maxWhatsappMonthly(300)
                .maxSmsMonthly(0)
                .features(List.of(
                        PlanFeature.ONLINE_BOOKING,
                        PlanFeature.WHATSAPP_NOTIFICATIONS,
                        PlanFeature.PACKAGE_MANAGEMENT,
                        PlanFeature.NO_SHOW_TRACKING,
                        PlanFeature.CUSTOMER_NOTES,
                        PlanFeature.BASIC_REPORTS
                ))
                .monthlyPrice(new BigDecimal("690.00"))
                .yearlyPrice(new BigDecimal("6900.00"))
                .build();
    }

    private Plan buildProPlan() {
        return Plan.builder()
                .code(PlanCode.PRO)
                .maxEmployees(5)
                .maxAppointmentsMonthly(800)
                .maxServices(25)
                .maxBranches(1)
                .maxWhatsappMonthly(1500)
                .maxSmsMonthly(0)
                .features(List.of(
                        PlanFeature.ONLINE_BOOKING,
                        PlanFeature.REMOVE_BRANDING,
                        PlanFeature.WHATSAPP_NOTIFICATIONS,
                        PlanFeature.PACKAGE_MANAGEMENT,
                        PlanFeature.NO_SHOW_TRACKING,
                        PlanFeature.CUSTOMER_NOTES,
                        PlanFeature.BASIC_REPORTS,
                        PlanFeature.ADVANCED_REPORTS,
                        PlanFeature.EXPORT_REPORTS,
                        PlanFeature.PRIORITY_SUPPORT,
                        PlanFeature.WAITLIST,
                        PlanFeature.CUSTOMER_REVIEWS,
                        PlanFeature.DEPOSIT_PAYMENTS,
                        PlanFeature.CALENDAR_SYNC,
                        PlanFeature.MINI_WEBSITE
                ))
                .monthlyPrice(new BigDecimal("1190.00"))
                .yearlyPrice(new BigDecimal("11900.00"))
                .build();
    }

    private Plan buildEnterprisePlan() {
        return Plan.builder()
                .code(PlanCode.ENTERPRISE)
                .maxEmployees(20)
                .maxAppointmentsMonthly(-1)
                .maxServices(-1)
                .maxBranches(5)
                .maxWhatsappMonthly(10000)
                .maxSmsMonthly(0)
                .features(List.of(
                        PlanFeature.ONLINE_BOOKING,
                        PlanFeature.REMOVE_BRANDING,
                        PlanFeature.CUSTOM_DOMAIN,
                        PlanFeature.WHATSAPP_NOTIFICATIONS,
                        PlanFeature.PACKAGE_MANAGEMENT,
                        PlanFeature.NO_SHOW_TRACKING,
                        PlanFeature.CUSTOMER_NOTES,
                        PlanFeature.BASIC_REPORTS,
                        PlanFeature.ADVANCED_REPORTS,
                        PlanFeature.EXPORT_REPORTS,
                        PlanFeature.PRIORITY_SUPPORT,
                        PlanFeature.SUPPORT_24_7,
                        PlanFeature.API_ACCESS,
                        PlanFeature.WAITLIST,
                        PlanFeature.CUSTOMER_REVIEWS,
                        PlanFeature.DEPOSIT_PAYMENTS,
                        PlanFeature.CALENDAR_SYNC,
                        PlanFeature.MINI_WEBSITE
                ))
                .monthlyPrice(new BigDecimal("1990.00"))
                .yearlyPrice(new BigDecimal("19900.00"))
                .build();
    }
}
