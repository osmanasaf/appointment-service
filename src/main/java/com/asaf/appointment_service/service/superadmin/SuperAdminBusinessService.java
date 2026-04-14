package com.asaf.appointment_service.service.superadmin;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import com.asaf.appointment_service.dto.superadmin.SuperAdminBusinessDetailResponse;
import com.asaf.appointment_service.dto.superadmin.SuperAdminBusinessListResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.PlanLimitUsage;
import com.asaf.appointment_service.entity.user.User;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.repository.business.BusinessRepository;
import com.asaf.appointment_service.repository.employee.EmployeeRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanLimitUsageRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.repository.service.ServiceRepository;
import com.asaf.appointment_service.repository.user.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuperAdminBusinessService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceRepository serviceRepository;
    private final BusinessSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PlanLimitUsageRepository usageRepository;

    public SuperAdminBusinessService(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            ServiceRepository serviceRepository,
            BusinessSubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            PlanLimitUsageRepository usageRepository) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.serviceRepository = serviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.usageRepository = usageRepository;
    }

    @Transactional(readOnly = true)
    public Page<SuperAdminBusinessListResponse> listBusinesses(String search, Pageable pageable) {
        Page<Business> businessPage;
        if (search != null && !search.isBlank()) {
            businessPage = businessRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            businessPage = businessRepository.findAll(pageable);
        }

        List<SuperAdminBusinessListResponse> content = businessPage.getContent().stream()
            .map(this::mapToListResponse)
            .toList();

        return new PageImpl<>(content, pageable, businessPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SuperAdminBusinessDetailResponse getBusinessDetail(Long businessId) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("İşletme", businessId));

        User owner = findBusinessOwner(businessId);
        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(businessId).orElse(null);
        Plan plan = subscription != null
            ? planRepository.findById(subscription.getPlanId()).orElse(null)
            : null;

        int employeeCount = (int) employeeRepository.countByBusinessId(businessId);
        int serviceCount = (int) serviceRepository.countByBusinessId(businessId);

        YearMonth now = YearMonth.now();
        PlanLimitUsage usage = usageRepository
            .findByBusinessIdAndYearAndMonth(businessId, now.getYear(), now.getMonthValue())
            .orElse(null);

        int monthAppointments = usage != null ? usage.getAppointmentsCount() : 0;
        int monthSms = usage != null ? usage.getSmsSent() : 0;
        int monthWhatsapp = usage != null ? usage.getWhatsappSent() : 0;

        int maxEmployees = plan != null ? plan.getMaxEmployees() : 0;
        int maxAppointments = plan != null ? plan.getMaxAppointmentsMonthly() : 0;
        int maxServices = plan != null ? plan.getMaxServices() : 0;
        int maxWhatsapp = plan != null ? plan.getMaxWhatsappMonthly() : 0;
        int maxSms = plan != null ? plan.getMaxSmsMonthly() : 0;

        return new SuperAdminBusinessDetailResponse(
            business.getId(),
            business.getName(),
            business.getSlug(),
            business.getCategory().name(),
            business.getStatus().name(),
            business.getCreatedAt(),
            owner != null ? owner.getName() : "—",
            owner != null ? owner.getEmail() : "—",
            plan != null ? plan.getCode().name() : "—",
            subscription != null ? subscription.getStatus().name() : "—",
            subscription != null ? subscription.getCurrentPeriodStart() : null,
            subscription != null ? subscription.getCurrentPeriodEnd() : null,
            subscription != null ? subscription.getTrialEndDate() : null,
            subscription != null && subscription.isManual(),
            maxEmployees,
            maxAppointments,
            maxServices,
            maxWhatsapp,
            maxSms,
            employeeCount,
            monthAppointments,
            monthSms,
            monthWhatsapp,
            serviceCount,
            calculateRate(employeeCount, maxEmployees),
            calculateRate(monthAppointments, maxAppointments),
            calculateRate(serviceCount, maxServices)
        );
    }

    @Transactional
    public void changePlan(Long businessId, String planCodeStr, Integer periodMonths) {
        businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("İşletme", businessId));

        PlanCode planCode = PlanCode.valueOf(planCodeStr);
        Plan plan = planRepository.findByCode(planCode)
            .orElseThrow(() -> new EntityNotFoundException("Plan", planCodeStr));

        int months = periodMonths != null ? periodMonths : 1;
        LocalDate periodStart = LocalDate.now();
        LocalDate periodEnd = periodStart.plusMonths(months);

        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(businessId)
            .orElse(null);

        if (subscription != null) {
            subscription.switchPlan(plan.getId(), true, periodStart, periodEnd);
        } else {
            subscription = BusinessSubscription.builder()
                .businessId(businessId)
                .planId(plan.getId())
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .manual(true)
                .build();
            subscription.switchPlan(plan.getId(), true, periodStart, periodEnd);
        }

        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void suspendBusiness(Long businessId) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("İşletme", businessId));
        business.suspend();
        businessRepository.save(business);

        subscriptionRepository.findByBusinessId(businessId)
            .ifPresent(sub -> {
                sub.suspend();
                subscriptionRepository.save(sub);
            });
    }

    @Transactional
    public void activateBusiness(Long businessId) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("İşletme", businessId));
        business.activate();
        businessRepository.save(business);
    }

    private SuperAdminBusinessListResponse mapToListResponse(Business business) {
        User owner = findBusinessOwner(business.getId());
        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(business.getId()).orElse(null);
        Plan plan = subscription != null
            ? planRepository.findById(subscription.getPlanId()).orElse(null)
            : null;

        int employeeCount = (int) employeeRepository.countByBusinessId(business.getId());

        YearMonth now = YearMonth.now();
        PlanLimitUsage usage = usageRepository
            .findByBusinessIdAndYearAndMonth(business.getId(), now.getYear(), now.getMonthValue())
            .orElse(null);
        int monthAppointments = usage != null ? usage.getAppointmentsCount() : 0;

        return new SuperAdminBusinessListResponse(
            business.getId(),
            business.getName(),
            business.getSlug(),
            business.getCategory().getLabel(),
            owner != null ? owner.getName() : "—",
            owner != null ? owner.getEmail() : "—",
            plan != null ? plan.getCode().name() : "—",
            subscription != null ? subscription.getStatus().name() : "—",
            subscription != null ? subscription.getCurrentPeriodEnd() : null,
            employeeCount,
            monthAppointments,
            business.getCreatedAt()
        );
    }

    private User findBusinessOwner(Long businessId) {
        List<User> users = userRepository.findByBusinessId(businessId);
        return users.stream()
            .filter(u -> u.getRole() == UserRole.BUSINESS_OWNER || u.getRole() == UserRole.ADMIN)
            .findFirst()
            .orElse(users.isEmpty() ? null : users.getFirst());
    }

    private double calculateRate(int current, int max) {
        if (max <= 0) return 0.0;
        return Math.min(100.0, (double) current / max * 100);
    }
}
