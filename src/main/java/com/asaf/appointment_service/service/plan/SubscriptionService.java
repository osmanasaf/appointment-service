package com.asaf.appointment_service.service.plan;

import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.PlanLimitUsage;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import com.asaf.appointment_service.exception.plan.PlanFeatureNotAvailableException;
import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import com.asaf.appointment_service.exception.plan.PlanNotFoundException;
import com.asaf.appointment_service.repository.employee.EmployeeRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanLimitUsageRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.repository.service.ServiceRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final int TRIAL_DAYS = 14;

    private final PlanRepository planRepository;
    private final BusinessSubscriptionRepository subscriptionRepository;
    private final PlanLimitUsageRepository usageRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessService businessService;

    public SubscriptionService(PlanRepository planRepository,
                               BusinessSubscriptionRepository subscriptionRepository,
                               PlanLimitUsageRepository usageRepository,
                               EmployeeRepository employeeRepository,
                               ServiceRepository serviceRepository,
                               BusinessService businessService) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageRepository = usageRepository;
        this.employeeRepository = employeeRepository;
        this.serviceRepository = serviceRepository;
        this.businessService = businessService;
    }

    @Transactional
    public List<Plan> getPlans() {
        ensureDefaultPlansExist();
        return planRepository.findAllByOrderByMonthlyPriceAsc();
    }

    @Transactional
    public BusinessSubscription initializeTrialSubscription(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        return subscriptionRepository.findByBusinessId(businessId)
            .map(this::refreshTrialStatusIfNeeded)
            .orElseGet(() -> {
                Plan trialPlan = findPlanByCode(PlanCode.SOLO);
                LocalDate today = utcToday();
                LocalDate trialEnd = today.plusDays(TRIAL_DAYS);
                BusinessSubscription subscription = BusinessSubscription.builder()
                    .businessId(businessId)
                    .planId(trialPlan.getId())
                    .status(SubscriptionStatus.TRIAL)
                    .trialEndDate(trialEnd)
                    .currentPeriodStart(today)
                    .currentPeriodEnd(trialEnd)
                    .manual(true)
                    .build();
                return subscriptionRepository.save(subscription);
            });
    }

    @Transactional
    public BusinessSubscription getCurrentSubscriptionByBusinessId(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(businessId)
            .orElseGet(() -> initializeTrialSubscription(businessId));
        return refreshTrialStatusIfNeeded(subscription);
    }

    @Transactional(readOnly = true)
    public Plan getCurrentPlanByBusinessId(Long businessId) {
        BusinessSubscription subscription = getCurrentSubscriptionByBusinessId(businessId);
        return planRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new EntityNotFoundException("Plan", subscription.getPlanId()));
    }

    @Transactional(readOnly = true)
    public PlanLimitUsage getCurrentUsage(Long businessId) {
        YearMonth period = utcYearMonth();
        return usageRepository.findByBusinessIdAndYearAndMonth(businessId, period.getYear(), period.getMonthValue())
            .orElseGet(() -> PlanLimitUsage.builder()
                .businessId(businessId)
                .year(period.getYear())
                .month(period.getMonthValue())
                .appointmentsCount(0)
                .smsSent(0)
                .whatsappSent(0)
                .build());
    }

    @Transactional
    public BusinessSubscription assignPlanToBusiness(Long businessId, PlanCode planCode, boolean manual) {
        businessService.findByIdOrThrow(businessId);
        Plan newPlan = findPlanByCode(planCode);
        LocalDate today = utcToday();
        LocalDate periodEnd = today.plusMonths(1);

        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(businessId)
            .orElseGet(() -> BusinessSubscription.builder()
                .businessId(businessId)
                .planId(newPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(today)
                .currentPeriodEnd(periodEnd)
                .manual(manual)
                .build());

        subscription.switchPlan(newPlan.getId(), manual, today, periodEnd);
        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(Long businessId, String featureCode) {
        return getCurrentPlanByBusinessId(businessId).hasFeature(featureCode);
    }

    @Transactional(readOnly = true)
    public void requireFeature(Long businessId, String featureCode) {
        if (!isFeatureEnabled(businessId, featureCode)) {
            throw new PlanFeatureNotAvailableException(featureCode);
        }
    }

    @Transactional(readOnly = true)
    public void checkEmployeeLimit(Long businessId) {
        Plan plan = getCurrentPlanByBusinessId(businessId);
        validateSubscriptionUsable(businessId);
        if (plan.isUnlimitedEmployees()) {
            return;
        }
        long existingEmployeeCount = employeeRepository.countByBusinessId(businessId);
        if (existingEmployeeCount >= plan.getMaxEmployees()) {
            throw new PlanLimitExceededException("Çalışan limitiniz doldu. Planınızı yükseltin.");
        }
    }

    @Transactional(readOnly = true)
    public void checkServiceLimit(Long businessId) {
        Plan plan = getCurrentPlanByBusinessId(businessId);
        validateSubscriptionUsable(businessId);
        if (plan.isUnlimitedServices()) {
            return;
        }
        long existingServiceCount = serviceRepository.countByBusinessId(businessId);
        if (existingServiceCount >= plan.getMaxServices()) {
            throw new PlanLimitExceededException("Hizmet limitiniz doldu. Planınızı yükseltin.");
        }
    }

    @Transactional(readOnly = true)
    public void checkAppointmentLimit(Long businessId) {
        Plan plan = getCurrentPlanByBusinessId(businessId);
        validateSubscriptionUsable(businessId);
        if (plan.isUnlimitedAppointments()) {
            return;
        }
        PlanLimitUsage usage = getCurrentUsage(businessId);
        if (usage.getAppointmentsCount() >= plan.getMaxAppointmentsMonthly()) {
            throw new PlanLimitExceededException("Aylık randevu limitiniz doldu. Planınızı yükseltin.");
        }
    }

    /**
     * Dönem ortasında plan yükseltir. Kalan süreye orantılı fiyat farkını döner
     * (ödeme akışında bu tutar kullanılır).
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUpgradeProration(Long businessId, PlanCode targetPlanCode,
                                                BillingPeriod billingPeriod) {
        BusinessSubscription subscription = getCurrentSubscriptionByBusinessId(businessId);
        Plan currentPlan = getCurrentPlanByBusinessId(businessId);
        Plan targetPlan = findPlanByCode(targetPlanCode);

        BigDecimal targetPrice = billingPeriod == BillingPeriod.YEARLY
                ? targetPlan.getYearlyPrice() : targetPlan.getMonthlyPrice();
        BigDecimal currentPrice = billingPeriod == BillingPeriod.YEARLY
                ? currentPlan.getYearlyPrice() : currentPlan.getMonthlyPrice();

        if (targetPrice.compareTo(currentPrice) <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate today = utcToday();
        LocalDate periodEnd = subscription.getCurrentPeriodEnd();
        long totalDays = ChronoUnit.DAYS.between(subscription.getCurrentPeriodStart(), periodEnd) + 1;
        long remainingDays = ChronoUnit.DAYS.between(today, periodEnd) + 1;

        if (totalDays <= 0 || remainingDays <= 0) {
            return targetPrice.subtract(currentPrice);
        }

        BigDecimal ratio = BigDecimal.valueOf(remainingDays)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
        BigDecimal priceDiff = targetPrice.subtract(currentPrice);
        return priceDiff.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public BusinessSubscription cancelSubscription(Long businessId) {
        businessService.findByIdOrThrow(businessId);
        BusinessSubscription subscription = getCurrentSubscriptionByBusinessId(businessId);
        subscription.cancelAutoRenew();
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public BusinessSubscription toggleAutoRenew(Long businessId, boolean enabled) {
        businessService.findByIdOrThrow(businessId);
        BusinessSubscription subscription = getCurrentSubscriptionByBusinessId(businessId);
        if (enabled) {
            LocalDate nextPayment = subscription.getCurrentPeriodEnd().plusDays(1);
            subscription.enableAutoRenew(nextPayment);
        } else {
            subscription.cancelAutoRenew();
        }
        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<BusinessSubscription> findSubscriptionsDueForRenewal(LocalDate date) {
        return subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).stream()
                .filter(sub -> sub.isAutoRenew()
                        && sub.hasStoredPaymentMethod()
                        && sub.getNextPaymentDate() != null
                        && !sub.getNextPaymentDate().isAfter(date))
                .toList();
    }

    @Transactional
    public void incrementAppointmentUsage(Long businessId) {
        YearMonth period = utcYearMonth();
        PlanLimitUsage usage = usageRepository.findByBusinessIdAndYearAndMonth(
                businessId,
                period.getYear(),
                period.getMonthValue()
            )
            .orElseGet(() -> PlanLimitUsage.builder()
                .businessId(businessId)
                .year(period.getYear())
                .month(period.getMonthValue())
                .appointmentsCount(0)
                .smsSent(0)
                .whatsappSent(0)
                .build());
        usage.incrementAppointments();
        usageRepository.save(usage);
    }

    private Plan findPlanByCode(PlanCode code) {
        ensureDefaultPlansExist();
        return planRepository.findByCode(code)
            .orElseThrow(() -> new PlanNotFoundException(code));
    }

    private void ensureDefaultPlansExist() {
        // PlanSeeder tarafından oluşturuluyor, burada sadece kontrol yapıyoruz
        if (planRepository.count() == 0) {
            throw new IllegalStateException("Planlar henüz oluşturulmamış. PlanSeeder çalışmalı.");
        }
    }

    private void validateSubscriptionUsable(Long businessId) {
        BusinessSubscription subscription = getCurrentSubscriptionByBusinessId(businessId);
        if (!subscription.isUsable()) {
            throw new PlanLimitExceededException("Aboneliğiniz aktif değil. Planınızı yenileyin.");
        }
    }

    private BusinessSubscription refreshTrialStatusIfNeeded(BusinessSubscription subscription) {
        LocalDate today = utcToday();
        if (subscription.isTrialExpired(today)) {
            subscription.expire();
            return subscriptionRepository.save(subscription);
        }
        return subscription;
    }

    private LocalDate utcToday() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private YearMonth utcYearMonth() {
        return YearMonth.now(ZoneOffset.UTC);
    }
}
