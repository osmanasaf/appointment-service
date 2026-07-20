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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Her ayın başında, ADVANCED_REPORTS özelliğine sahip işletmelere geçen ayın
 * performans özetini WhatsApp ile gönderir. İşletmenin WhatsApp kotasından düşülmez.
 */
@Service
public class MonthlyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportScheduler.class);

    private final BusinessService businessService;
    private final SubscriptionService subscriptionService;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationLogRepository notificationLogRepository;
    private final ReportService reportService;
    private final NotificationService notificationService;

    public MonthlyReportScheduler(BusinessService businessService,
                                  SubscriptionService subscriptionService,
                                  NotificationSettingsService notificationSettingsService,
                                  NotificationLogRepository notificationLogRepository,
                                  ReportService reportService,
                                  NotificationService notificationService) {
        this.businessService = businessService;
        this.subscriptionService = subscriptionService;
        this.notificationSettingsService = notificationSettingsService;
        this.notificationLogRepository = notificationLogRepository;
        this.reportService = reportService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${monthly.report.scheduler.cron:0 0 9 1 * *}")
    public void sendMonthlySummaries() {
        LocalDate previousMonthStart = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate previousMonthEnd = previousMonthStart.withDayOfMonth(previousMonthStart.lengthOfMonth());

        List<Business> businesses = businessService.findAllActive();
        int sentCount = 0;
        for (Business business : businesses) {
            try {
                if (sendSummaryIfEligible(business, previousMonthStart, previousMonthEnd)) {
                    sentCount++;
                }
            } catch (Exception e) {
                log.error("Aylık özet gönderilemedi: businessId={}, error={}", business.getId(), e.getMessage());
            }
        }
        log.info("Aylık özet scheduler tamamlandı: {} işletmeye gönderildi", sentCount);
    }

    private boolean sendSummaryIfEligible(Business business, LocalDate periodStart, LocalDate periodEnd) {
        Long businessId = business.getId();
        if (!subscriptionService.isFeatureEnabled(businessId, PlanFeature.ADVANCED_REPORTS)) {
            return false;
        }
        if (alreadySentThisMonth(businessId)) {
            return false;
        }
        NotificationSettings settings = notificationSettingsService.getSettings(businessId);
        if (!settings.isMonthlySummaryEnabled()) {
            return false;
        }
        String phone = business.getFormattedPhoneNumber();
        if (phone == null || phone.isBlank()) {
            log.warn("Aylık özet atlandı, işletme telefonu tanımlı değil: businessId={}", businessId);
            return false;
        }

        ReportDetailedResponse report = reportService.getDetailed(businessId, periodStart, periodEnd, null);
        BigDecimal recoveredRevenue = reportService.calculateWaitlistRecoveredRevenue(businessId, periodStart, periodEnd);

        notificationService.sendMonthlySummary(
            businessId, phone, report.totalAppointments(), report.noShowRatePercent(),
            report.previousPeriodNoShowRatePercent(), recoveredRevenue);
        return true;
    }

    private boolean alreadySentThisMonth(Long businessId) {
        return notificationLogRepository.existsByBusinessIdAndTypeAndCreatedAtAfter(
            businessId, NotificationType.MONTHLY_SUMMARY, LocalDate.now().withDayOfMonth(1).atStartOfDay());
    }
}
