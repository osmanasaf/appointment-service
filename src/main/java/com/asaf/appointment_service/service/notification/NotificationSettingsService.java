package com.asaf.appointment_service.service.notification;

import com.asaf.appointment_service.entity.notification.NotificationSettings;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.repository.notification.NotificationSettingsRepository;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class NotificationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSettingsService.class);

    private final NotificationSettingsRepository repository;
    private final SubscriptionService subscriptionService;

    public NotificationSettingsService(NotificationSettingsRepository repository,
                                        SubscriptionService subscriptionService) {
        this.repository = repository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(readOnly = true)
    public NotificationSettings getSettings(Long businessId) {
        return repository.findByBusinessId(businessId)
                .orElseGet(() -> initializeSettings(businessId));
    }

    @Transactional
    public NotificationSettings initializeSettings(Long businessId) {
        if (repository.existsByBusinessId(businessId)) {
            return repository.findByBusinessId(businessId).orElseThrow();
        }
        
        NotificationSettings settings = NotificationSettings.createDefault(businessId);
        return repository.save(settings);
    }

    @Transactional
    public NotificationSettings updateWhatsAppSettings(Long businessId, boolean enabled,
                                                        boolean reminderEnabled,
                                                        boolean confirmationEnabled,
                                                        boolean cancellationEnabled) {
        NotificationSettings settings = getSettings(businessId);
        
        // Plan kontrolü - WhatsApp özelliği var mı?
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        if (!plan.hasFeature(PlanFeature.WHATSAPP_NOTIFICATIONS) && enabled) {
            throw new IllegalStateException("Mevcut planınız WhatsApp bildirimlerini desteklemiyor. " +
                    "Lütfen planınızı yükseltin.");
        }
        
        settings.updateWhatsAppSettings(enabled, reminderEnabled, confirmationEnabled, cancellationEnabled);
        return repository.save(settings);
    }

    @Transactional
    public NotificationSettings updateSmsSettings(Long businessId, boolean enabled, boolean fallbackEnabled) {
        NotificationSettings settings = getSettings(businessId);
        
        // Plan kontrolü - SMS özelliği var mı?
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        if (!plan.hasFeature(PlanFeature.SMS_NOTIFICATIONS) && enabled) {
            throw new IllegalStateException("Mevcut planınız SMS bildirimlerini desteklemiyor. " +
                    "Lütfen planınızı yükseltin.");
        }
        
        settings.updateSmsSettings(enabled, fallbackEnabled);
        return repository.save(settings);
    }

    @Transactional
    public NotificationSettings updateReminderSettings(Long businessId, int hoursBefore,
                                                        boolean secondEnabled, Integer secondHoursBefore) {
        NotificationSettings settings = getSettings(businessId);
        settings.updateReminderSettings(hoursBefore, secondEnabled, secondHoursBefore);
        return repository.save(settings);
    }

    @Transactional
    public NotificationSettings updateAllSettings(Long businessId,
                                                   boolean whatsappEnabled,
                                                   boolean whatsappReminderEnabled,
                                                   boolean whatsappConfirmationEnabled,
                                                   boolean whatsappCancellationEnabled,
                                                   boolean smsEnabled,
                                                   boolean smsFallbackEnabled,
                                                   int reminderHoursBefore,
                                                   boolean secondReminderEnabled,
                                                   Integer secondReminderHoursBefore) {
        NotificationSettings settings = getSettings(businessId);
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        
        // WhatsApp kontrolü
        if (!plan.hasFeature(PlanFeature.WHATSAPP_NOTIFICATIONS) && whatsappEnabled) {
            throw new IllegalStateException("Mevcut planınız WhatsApp bildirimlerini desteklemiyor.");
        }
        
        // SMS kontrolü
        if (!plan.hasFeature(PlanFeature.SMS_NOTIFICATIONS) && smsEnabled) {
            throw new IllegalStateException("Mevcut planınız SMS bildirimlerini desteklemiyor.");
        }
        
        settings.updateWhatsAppSettings(whatsappEnabled, whatsappReminderEnabled, 
                whatsappConfirmationEnabled, whatsappCancellationEnabled);
        settings.updateSmsSettings(smsEnabled, smsFallbackEnabled);
        settings.updateReminderSettings(reminderHoursBefore, secondReminderEnabled, secondReminderHoursBefore);
        
        return repository.save(settings);
    }

    /**
     * WhatsApp mesaj gönderildiğinde kullanımı artır.
     * @return true eğer limit aşılmadıysa, false eğer limit aşıldıysa
     */
    @Transactional
    public boolean incrementWhatsappUsage(Long businessId) {
        NotificationSettings settings = getSettings(businessId);
        checkAndResetMonthlyUsage(settings);
        
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        int limit = plan.getMaxWhatsappMonthly();
        
        if (limit >= 0 && settings.getWhatsappUsedThisMonth() >= limit) {
            log.warn("Business {} WhatsApp limit aşıldı: {}/{}", businessId, 
                    settings.getWhatsappUsedThisMonth(), limit);
            return false;
        }
        
        settings.incrementWhatsappUsage();
        repository.save(settings);
        return true;
    }

    /**
     * SMS gönderildiğinde kullanımı artır.
     * @return true eğer limit aşılmadıysa, false eğer limit aşıldıysa
     */
    @Transactional
    public boolean incrementSmsUsage(Long businessId) {
        NotificationSettings settings = getSettings(businessId);
        checkAndResetMonthlyUsage(settings);
        
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        int limit = plan.getMaxSmsMonthly();
        
        if (limit >= 0 && settings.getSmsUsedThisMonth() >= limit) {
            log.warn("Business {} SMS limit aşıldı: {}/{}", businessId, 
                    settings.getSmsUsedThisMonth(), limit);
            return false;
        }
        
        settings.incrementSmsUsage();
        repository.save(settings);
        return true;
    }

    /**
     * Kalan WhatsApp mesaj hakkını döner.
     * -1 = sınırsız
     */
    @Transactional(readOnly = true)
    public int getRemainingWhatsappQuota(Long businessId) {
        NotificationSettings settings = getSettings(businessId);
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        
        int limit = plan.getMaxWhatsappMonthly();
        if (limit < 0) return -1; // Sınırsız
        
        return Math.max(0, limit - settings.getWhatsappUsedThisMonth());
    }

    /**
     * Kalan SMS hakkını döner.
     * -1 = sınırsız
     */
    @Transactional(readOnly = true)
    public int getRemainingSmsQuota(Long businessId) {
        NotificationSettings settings = getSettings(businessId);
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        
        int limit = plan.getMaxSmsMonthly();
        if (limit < 0) return -1; // Sınırsız
        
        return Math.max(0, limit - settings.getSmsUsedThisMonth());
    }

    private void checkAndResetMonthlyUsage(NotificationSettings settings) {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        if (settings.getUsageResetMonth() == null || 
            settings.getUsageResetYear() == null ||
            settings.getUsageResetMonth() != currentMonth || 
            settings.getUsageResetYear() != currentYear) {
            settings.resetMonthlyUsage(currentMonth, currentYear);
            repository.save(settings);
        }
    }
}
