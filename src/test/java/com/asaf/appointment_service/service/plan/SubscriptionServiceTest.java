package com.asaf.appointment_service.service.plan;

import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.PlanLimitUsage;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import com.asaf.appointment_service.repository.employee.EmployeeRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanLimitUsageRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.repository.service.ServiceRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private BusinessSubscriptionRepository subscriptionRepository;
    @Mock
    private PlanLimitUsageRepository usageRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private BusinessService businessService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void should_throwPlanLimitExceeded_when_monthlyAppointmentLimitReached() {
        Long businessId = 10L;
        Plan plan = createPlan(PlanCode.STARTER, 1L, 1, 1, 1);
        BusinessSubscription subscription = BusinessSubscription.builder()
            .businessId(businessId)
            .planId(plan.getId())
            .status(SubscriptionStatus.ACTIVE)
            .currentPeriodStart(LocalDate.now(ZoneOffset.UTC))
            .currentPeriodEnd(LocalDate.now(ZoneOffset.UTC).plusMonths(1))
            .manual(true)
            .build();
        YearMonth period = YearMonth.now(ZoneOffset.UTC);
        PlanLimitUsage usage = PlanLimitUsage.builder()
            .businessId(businessId)
            .year(period.getYear())
            .month(period.getMonthValue())
            .appointmentsCount(1)
            .smsSent(0)
            .whatsappSent(0)
            .build();

        when(subscriptionRepository.findByBusinessId(businessId)).thenReturn(Optional.of(subscription));
        when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(usageRepository.findByBusinessIdAndYearAndMonth(businessId, period.getYear(), period.getMonthValue()))
            .thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> subscriptionService.checkAppointmentLimit(businessId))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("Aylık randevu limitiniz doldu");
    }

    @Test
    void should_markSubscriptionExpired_when_trialEndDatePassed() {
        Long businessId = 20L;
        BusinessSubscription trialSubscription = BusinessSubscription.builder()
            .businessId(businessId)
            .planId(1L)
            .status(SubscriptionStatus.TRIAL)
            .trialEndDate(LocalDate.now(ZoneOffset.UTC).minusDays(1))
            .currentPeriodStart(LocalDate.now(ZoneOffset.UTC).minusDays(14))
            .currentPeriodEnd(LocalDate.now(ZoneOffset.UTC).minusDays(1))
            .manual(true)
            .build();

        when(subscriptionRepository.findByBusinessId(businessId)).thenReturn(Optional.of(trialSubscription));
        when(subscriptionRepository.save(trialSubscription)).thenReturn(trialSubscription);

        BusinessSubscription updated = subscriptionService.getCurrentSubscriptionByBusinessId(businessId);

        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).save(trialSubscription);
    }

    @Test
    void should_switchPlanAndActivateSubscription_when_assignPlanToBusinessCalled() {
        Long businessId = 30L;
        Plan proPlan = createPlan(PlanCode.PRO, 2L, 5, 750, 30);
        BusinessSubscription subscription = BusinessSubscription.builder()
            .businessId(businessId)
            .planId(1L)
            .status(SubscriptionStatus.TRIAL)
            .trialEndDate(LocalDate.now(ZoneOffset.UTC).plusDays(10))
            .currentPeriodStart(LocalDate.now(ZoneOffset.UTC))
            .currentPeriodEnd(LocalDate.now(ZoneOffset.UTC).plusDays(10))
            .manual(true)
            .build();

        when(planRepository.count()).thenReturn(4L); // PlanSeeder tarafından oluşturulmuş gibi
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findByBusinessId(businessId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        BusinessSubscription updated = subscriptionService.assignPlanToBusiness(businessId, PlanCode.PRO, true);

        assertThat(updated.getPlanId()).isEqualTo(proPlan.getId());
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getTrialEndDate()).isNull();
        assertThat(updated.isManual()).isTrue();
    }

    private Plan createPlan(PlanCode code, Long id, int maxEmployees, int maxAppointmentsMonthly, int maxServices) {
        Plan plan = Plan.builder()
            .code(code)
            .maxEmployees(maxEmployees)
            .maxAppointmentsMonthly(maxAppointmentsMonthly)
            .maxServices(maxServices)
            .maxBranches(1)
            .maxWhatsappMonthly(500)
            .maxSmsMonthly(100)
            .features(List.of("WHATSAPP_NOTIFICATIONS"))
            .monthlyPrice(BigDecimal.valueOf(499))
            .yearlyPrice(BigDecimal.valueOf(4790.40))
            .build();
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }
}
