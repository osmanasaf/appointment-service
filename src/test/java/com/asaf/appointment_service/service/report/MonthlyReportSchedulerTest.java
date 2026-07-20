package com.asaf.appointment_service.service.report;

import com.asaf.appointment_service.dto.report.ReportDetailedResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.notification.NotificationSettings;
import com.asaf.appointment_service.entity.notification.NotificationType;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.repository.notification.NotificationLogRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.notification.NotificationService;
import com.asaf.appointment_service.service.notification.NotificationSettingsService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyReportSchedulerTest {

    @Mock
    private BusinessService businessService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private NotificationSettingsService notificationSettingsService;
    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private ReportService reportService;
    @Mock
    private NotificationService notificationService;

    private MonthlyReportScheduler scheduler;

    private void setUp() {
        scheduler = new MonthlyReportScheduler(businessService, subscriptionService, notificationSettingsService,
            notificationLogRepository, reportService, notificationService);
    }

    private Business business(Long id, String phone) {
        return Business.builder().id(id).name("Kuaför X").slug("kuafor-x-" + id).phone("+90", phone).build();
    }

    private NotificationSettings settings(boolean enabled) {
        NotificationSettings settings = NotificationSettings.builder().businessId(1L)
            .monthlySummaryEnabled(enabled).build();
        return settings;
    }

    @Test
    void should_sendSummary_when_businessEligible() {
        setUp();
        Business business = business(1L, "5551234567");
        when(businessService.findAllActive()).thenReturn(List.of(business));
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.ADVANCED_REPORTS)).thenReturn(true);
        when(notificationLogRepository.existsByBusinessIdAndTypeAndCreatedAtAfter(
            eq(1L), eq(NotificationType.MONTHLY_SUMMARY), any())).thenReturn(false);
        when(notificationSettingsService.getSettings(1L)).thenReturn(settings(true));
        ReportDetailedResponse report = detailedResponse(42, 10.0, 5.0);
        when(reportService.getDetailed(eq(1L), any(), any(), eq(null))).thenReturn(report);
        when(reportService.calculateWaitlistRecoveredRevenue(eq(1L), any(), any()))
            .thenReturn(BigDecimal.valueOf(300));

        scheduler.sendMonthlySummaries();

        verify(notificationService).sendMonthlySummary(1L, "+905551234567", 42, 10.0, 5.0, BigDecimal.valueOf(300));
    }

    @Test
    void should_skip_when_advancedReportsFeatureDisabled() {
        setUp();
        Business business = business(1L, "5551234567");
        when(businessService.findAllActive()).thenReturn(List.of(business));
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.ADVANCED_REPORTS)).thenReturn(false);

        scheduler.sendMonthlySummaries();

        verify(notificationService, never()).sendMonthlySummary(anyLong(), any(), anyInt(), anyDouble(), any(), any());
    }

    @Test
    void should_skip_when_alreadySentThisMonth() {
        setUp();
        Business business = business(1L, "5551234567");
        when(businessService.findAllActive()).thenReturn(List.of(business));
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.ADVANCED_REPORTS)).thenReturn(true);
        when(notificationLogRepository.existsByBusinessIdAndTypeAndCreatedAtAfter(
            eq(1L), eq(NotificationType.MONTHLY_SUMMARY), any())).thenReturn(true);

        scheduler.sendMonthlySummaries();

        verify(notificationSettingsService, never()).getSettings(anyLong());
        verify(reportService, never()).getDetailed(anyLong(), any(), any(), any());
    }

    @Test
    void should_skip_when_monthlySummaryDisabledInSettings() {
        setUp();
        Business business = business(1L, "5551234567");
        when(businessService.findAllActive()).thenReturn(List.of(business));
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.ADVANCED_REPORTS)).thenReturn(true);
        when(notificationLogRepository.existsByBusinessIdAndTypeAndCreatedAtAfter(
            eq(1L), eq(NotificationType.MONTHLY_SUMMARY), any())).thenReturn(false);
        when(notificationSettingsService.getSettings(1L)).thenReturn(settings(false));

        scheduler.sendMonthlySummaries();

        verify(reportService, never()).getDetailed(anyLong(), any(), any(), any());
    }

    @Test
    void should_continueOtherBusinesses_when_oneBusinessThrows() {
        setUp();
        Business failing = business(1L, "5551234567");
        Business healthy = business(2L, "5559876543");
        when(businessService.findAllActive()).thenReturn(List.of(failing, healthy));
        when(subscriptionService.isFeatureEnabled(1L, PlanFeature.ADVANCED_REPORTS))
            .thenThrow(new RuntimeException("boom"));
        when(subscriptionService.isFeatureEnabled(2L, PlanFeature.ADVANCED_REPORTS)).thenReturn(true);
        when(notificationLogRepository.existsByBusinessIdAndTypeAndCreatedAtAfter(
            eq(2L), eq(NotificationType.MONTHLY_SUMMARY), any())).thenReturn(false);
        when(notificationSettingsService.getSettings(2L)).thenReturn(settings(true));
        ReportDetailedResponse report = detailedResponse(5, 0.0, null);
        when(reportService.getDetailed(eq(2L), any(), any(), eq(null))).thenReturn(report);
        when(reportService.calculateWaitlistRecoveredRevenue(eq(2L), any(), any())).thenReturn(BigDecimal.ZERO);

        scheduler.sendMonthlySummaries();

        verify(notificationService).sendMonthlySummary(2L, "+905559876543", 5, 0.0, null, BigDecimal.ZERO);
        verify(notificationService, never()).sendMonthlySummary(eq(1L), any(), anyInt(), anyDouble(), any(), any());
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static double anyDouble() {
        return org.mockito.ArgumentMatchers.anyDouble();
    }

    private ReportDetailedResponse detailedResponse(int total, double noShowRate, Double previousNoShowRate) {
        return new ReportDetailedResponse(
            LocalDate.now().minusMonths(1), LocalDate.now(), total, 0, 0, total, 0, 0,
            noShowRate, previousNoShowRate, 0.0, BigDecimal.ZERO, List.of(), List.of(), null, null, 0, 0);
    }
}
