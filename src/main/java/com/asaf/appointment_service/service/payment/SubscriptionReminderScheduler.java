package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.integration.email.EmailClient;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageRequest;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PayTR'de tekrarlayan ödeme yetkisi olmadığı için abonelikler peşin paket
 * (1/3/6/12 ay) olarak satılıyor. Bu scheduler dönem bitişine 7/3/1 gün kala
 * yenileme hatırlatması gönderir ve grace period sonunda süresi dolan
 * abonelikleri EXPIRED yapar.
 */
@Service
public class SubscriptionReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionReminderScheduler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final int[] REMINDER_THRESHOLDS_DAYS = {7, 3, 1};

    private final BusinessSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final BusinessService businessService;
    private final WhatsAppClientInterface whatsAppClient;
    private final EmailClient emailClient;
    private final EmailProperties emailProperties;

    @Value("${subscription.grace-period-days:3}")
    private int gracePeriodDays;

    public SubscriptionReminderScheduler(BusinessSubscriptionRepository subscriptionRepository,
                                         PlanRepository planRepository,
                                         BusinessService businessService,
                                         WhatsAppClientInterface whatsAppClient,
                                         EmailClient emailClient,
                                         EmailProperties emailProperties) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.businessService = businessService;
        this.whatsAppClient = whatsAppClient;
        this.emailClient = emailClient;
        this.emailProperties = emailProperties;
    }

    @Scheduled(cron = "${subscription.reminder.cron:0 0 9 * * *}", zone = "UTC")
    public void processSubscriptionReminders() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<BusinessSubscription> activeSubscriptions =
                subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);

        int reminderCount = 0;
        int expiredCount = 0;
        for (BusinessSubscription subscription : activeSubscriptions) {
            try {
                if (subscription.isGraceExpired(today, gracePeriodDays)) {
                    expireSubscription(subscription);
                    expiredCount++;
                } else if (sendReminderIfDue(subscription, today)) {
                    reminderCount++;
                }
            } catch (Exception e) {
                log.error("Abonelik hatırlatma/expiry işlemi başarısız: businessId={}",
                        subscription.getBusinessId(), e);
            }
        }

        log.info("Abonelik hatırlatma scheduler tamamlandı: {} hatırlatma, {} süresi doldu",
                reminderCount, expiredCount);
    }

    @Transactional
    protected void expireSubscription(BusinessSubscription subscription) {
        subscription.expire();
        subscriptionRepository.save(subscription);
        Business business = businessService.findByIdOrThrow(subscription.getBusinessId());
        String renewUrl = buildRenewUrl();
        if (business.getEmail() != null) {
            emailClient.sendSubscriptionExpired(business.getEmail(), business.getName(), renewUrl);
        }
        sendWhatsApp(business, WhatsAppMessageRequest.subscriptionExpired(
                business.getFormattedPhoneNumber(), business.getName(), renewUrl));
        log.warn("Abonelik süresi doldu: businessId={}", subscription.getBusinessId());
    }

    @Transactional
    protected boolean sendReminderIfDue(BusinessSubscription subscription, LocalDate today) {
        LocalDate periodEnd = subscription.getCurrentPeriodEnd();
        if (periodEnd == null) {
            return false;
        }
        long daysUntilEnd = ChronoUnit.DAYS.between(today, periodEnd);

        for (int threshold : REMINDER_THRESHOLDS_DAYS) {
            if (daysUntilEnd == threshold && !subscription.hasReminderBeenSentFor(threshold)) {
                sendReminder(subscription, threshold, periodEnd);
                subscription.markReminderSent(threshold);
                subscriptionRepository.save(subscription);
                return true;
            }
        }
        return false;
    }

    private void sendReminder(BusinessSubscription subscription, int daysBefore, LocalDate periodEnd) {
        Business business = businessService.findByIdOrThrow(subscription.getBusinessId());
        Plan plan = planRepository.findById(subscription.getPlanId()).orElse(null);
        String planName = plan != null ? plan.getCode().name() : "";
        String renewUrl = buildRenewUrl();
        String formattedEndDate = periodEnd.format(DATE_FORMATTER);

        if (business.getEmail() != null) {
            emailClient.sendSubscriptionRenewalReminder(
                    business.getEmail(), business.getName(), planName, formattedEndDate, daysBefore, renewUrl);
        }
        sendWhatsApp(business, WhatsAppMessageRequest.subscriptionRenewalReminder(
                business.getFormattedPhoneNumber(), business.getName(), planName,
                String.valueOf(daysBefore), formattedEndDate, renewUrl));

        log.info("Abonelik yenileme hatırlatması gönderildi: businessId={}, daysBefore={}",
                subscription.getBusinessId(), daysBefore);
    }

    private void sendWhatsApp(Business business, WhatsAppMessageRequest request) {
        if (!whatsAppClient.isEnabled() || business.getFormattedPhoneNumber() == null) {
            return;
        }
        whatsAppClient.sendMessage(request);
    }

    private String buildRenewUrl() {
        return emailProperties.baseUrl() + "/admin/settings/plan";
    }
}
