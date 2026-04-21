package com.asaf.appointment_service.service.superadmin;

import com.asaf.appointment_service.dto.superadmin.SuperAdminOverviewResponse;
import com.asaf.appointment_service.dto.superadmin.SuperAdminOverviewResponse.RegistrationTrendPoint;
import com.asaf.appointment_service.dto.superadmin.SuperAdminOverviewResponse.SubscriptionBreakdown;
import com.asaf.appointment_service.dto.superadmin.SuperAdminRevenueResponse;
import com.asaf.appointment_service.dto.superadmin.SuperAdminRevenueResponse.PlanRevenueBreakdown;
import com.asaf.appointment_service.dto.superadmin.WhatsAppAnalyticsResponse;
import com.asaf.appointment_service.dto.superadmin.WhatsAppAnalyticsResponse.BusinessWhatsAppUsage;
import com.asaf.appointment_service.dto.superadmin.WhatsAppAnalyticsResponse.DailyTrendPoint;
import com.asaf.appointment_service.dto.superadmin.WhatsAppAnalyticsResponse.TypeBreakdown;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessStatus;
import com.asaf.appointment_service.entity.notification.NotificationStatus;
import com.asaf.appointment_service.entity.notification.NotificationType;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.entity.user.UserRole;
import com.asaf.appointment_service.repository.business.BusinessRepository;
import com.asaf.appointment_service.repository.employee.EmployeeRepository;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuperAdminDashboardService {

    private static final int TREND_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final List<NotificationStatus> SUCCESS_STATUSES = List.of(
        NotificationStatus.SENT, NotificationStatus.DELIVERED, NotificationStatus.READ);

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BusinessSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final NotificationLogRepository notificationLogRepository;

    public SuperAdminDashboardService(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            BusinessSubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            NotificationLogRepository notificationLogRepository) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional(readOnly = true)
    public SuperAdminOverviewResponse getOverview() {
        long totalBusinesses = businessRepository.count();
        long activeBusinesses = businessRepository.countByStatus(BusinessStatus.ACTIVE);
        long totalUsers = userRepository.count() - userRepository.countByRole(UserRole.SUPER_ADMIN);
        long totalEmployees = employeeRepository.count();

        SubscriptionBreakdown subscriptionBreakdown = buildSubscriptionBreakdown();
        List<RegistrationTrendPoint> registrationTrend = buildRegistrationTrend();

        return new SuperAdminOverviewResponse(
            totalBusinesses,
            activeBusinesses,
            totalUsers,
            totalEmployees,
            subscriptionBreakdown,
            registrationTrend
        );
    }

    @Transactional(readOnly = true)
    public SuperAdminRevenueResponse getRevenue() {
        List<Plan> plans = planRepository.findAll();
        List<PlanRevenueBreakdown> breakdowns = new ArrayList<>();
        BigDecimal totalPotential = BigDecimal.ZERO;

        for (Plan plan : plans) {
            long activeCount = subscriptionRepository.countByPlanIdAndStatus(plan.getId(), SubscriptionStatus.ACTIVE);
            BigDecimal revenue = plan.getMonthlyPrice().multiply(BigDecimal.valueOf(activeCount));
            totalPotential = totalPotential.add(revenue);

            breakdowns.add(new PlanRevenueBreakdown(
                plan.getCode().name(),
                activeCount,
                plan.getMonthlyPrice(),
                revenue
            ));
        }

        return new SuperAdminRevenueResponse(totalPotential, breakdowns);
    }

    private SubscriptionBreakdown buildSubscriptionBreakdown() {
        return new SubscriptionBreakdown(
            subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL),
            subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE),
            subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED),
            subscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED)
        );
    }

    private List<RegistrationTrendPoint> buildRegistrationTrend() {
        List<RegistrationTrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            long totalBefore = businessRepository.count();
            long afterEnd = businessRepository.countByCreatedAtAfter(dayEnd);
            long afterStart = businessRepository.countByCreatedAtAfter(dayStart);
            long dayCount = afterStart - afterEnd;

            trend.add(new RegistrationTrendPoint(date.format(DATE_FORMAT), dayCount));
        }

        return trend;
    }

    @Transactional(readOnly = true)
    public WhatsAppAnalyticsResponse getWhatsAppAnalytics() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        long totalSent = notificationLogRepository.countByStatusInAndCreatedAtBetween(
            SUCCESS_STATUSES, monthStart, monthEnd);

        List<BusinessWhatsAppUsage> perBusiness = buildPerBusinessUsage(monthStart, monthEnd);
        List<TypeBreakdown> typeBreakdown = buildTypeBreakdown(monthStart, monthEnd);
        List<DailyTrendPoint> dailyTrend = buildDailyTrend();

        return new WhatsAppAnalyticsResponse(totalSent, perBusiness, typeBreakdown, dailyTrend);
    }

    private List<BusinessWhatsAppUsage> buildPerBusinessUsage(
            LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = notificationLogRepository.countByBusinessGrouped(
            SUCCESS_STATUSES, from, to);

        List<Long> businessIds = rows.stream()
            .map(row -> (Long) row[0])
            .toList();

        Map<Long, Business> businessMap = businessRepository.findAllById(businessIds).stream()
            .collect(Collectors.toMap(Business::getId, Function.identity()));

        return rows.stream()
            .map(row -> {
                Long bizId = (Long) row[0];
                long count = (Long) row[1];
                Business biz = businessMap.get(bizId);
                String name = biz != null ? biz.getName() : "Bilinmeyen";
                return new BusinessWhatsAppUsage(bizId, name, count);
            })
            .toList();
    }

    private List<TypeBreakdown> buildTypeBreakdown(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = notificationLogRepository.countByTypeGrouped(
            SUCCESS_STATUSES, from, to);

        return rows.stream()
            .map(row -> {
                NotificationType type = (NotificationType) row[0];
                long count = (Long) row[1];
                return new TypeBreakdown(type.name(), count);
            })
            .toList();
    }

    private List<DailyTrendPoint> buildDailyTrend() {
        LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(TREND_DAYS).atStartOfDay();
        List<Object[]> rows = notificationLogRepository.dailyCountSince(
            SUCCESS_STATUSES, thirtyDaysAgo);

        return rows.stream()
            .map(row -> {
                String date = row[0].toString();
                long count = (Long) row[1];
                return new DailyTrendPoint(date, count);
            })
            .toList();
    }
}
