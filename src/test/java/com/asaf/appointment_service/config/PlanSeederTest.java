package com.asaf.appointment_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class PlanSeederTest {

    @Mock
    private PlanRepository planRepository;

    private PlanSeeder planSeeder;

    private Plan planWithFeatures(PlanCode code, String... features) {
        return Plan.builder()
            .code(code)
            .maxEmployees(5)
            .maxAppointmentsMonthly(800)
            .maxServices(25)
            .maxBranches(1)
            .maxWhatsappMonthly(1500)
            .maxSmsMonthly(0)
            .features(List.of(features))
            .monthlyPrice(new BigDecimal("1190.00"))
            .yearlyPrice(new BigDecimal("11900.00"))
            .build();
    }

    @Test
    void should_seedAllThreePlans_when_databaseIsEmpty() {
        planSeeder = new PlanSeeder(planRepository);
        when(planRepository.count()).thenReturn(0L);

        planSeeder.run(new DefaultApplicationArguments(new String[0]));

        verify(planRepository, times(3)).save(any(Plan.class));
    }

    @Test
    void should_addMissingFeaturesToProAndEnterprise_when_plansAlreadyExistWithoutNewFeatures() {
        planSeeder = new PlanSeeder(planRepository);
        Plan proPlan = planWithFeatures(PlanCode.PRO, PlanFeature.ONLINE_BOOKING);
        Plan enterprisePlan = planWithFeatures(PlanCode.ENTERPRISE, PlanFeature.ONLINE_BOOKING);

        when(planRepository.count()).thenReturn(3L);
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(proPlan));
        when(planRepository.findByCode(PlanCode.ENTERPRISE)).thenReturn(Optional.of(enterprisePlan));

        planSeeder.run(new DefaultApplicationArguments(new String[0]));

        verify(planRepository).save(proPlan);
        verify(planRepository).save(enterprisePlan);
        assertThat(proPlan.hasFeature(PlanFeature.WAITLIST)).isTrue();
        assertThat(enterprisePlan.hasFeature(PlanFeature.MINI_WEBSITE)).isTrue();
    }

    @Test
    void should_notSave_when_plansAlreadyHaveAllNewFeatures() {
        planSeeder = new PlanSeeder(planRepository);
        Plan proPlan = planWithFeatures(PlanCode.PRO,
            PlanFeature.WAITLIST, PlanFeature.CUSTOMER_REVIEWS, PlanFeature.DEPOSIT_PAYMENTS,
            PlanFeature.CALENDAR_SYNC, PlanFeature.MINI_WEBSITE);
        Plan enterprisePlan = planWithFeatures(PlanCode.ENTERPRISE,
            PlanFeature.WAITLIST, PlanFeature.CUSTOMER_REVIEWS, PlanFeature.DEPOSIT_PAYMENTS,
            PlanFeature.CALENDAR_SYNC, PlanFeature.MINI_WEBSITE);

        when(planRepository.count()).thenReturn(3L);
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(proPlan));
        when(planRepository.findByCode(PlanCode.ENTERPRISE)).thenReturn(Optional.of(enterprisePlan));

        planSeeder.run(new DefaultApplicationArguments(new String[0]));

        verify(planRepository, never()).save(any(Plan.class));
    }
}
