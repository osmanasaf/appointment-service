package com.asaf.appointment_service.controller.notification;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.entity.notification.NotificationSettings;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.common.service.AuthenticatedBusinessService;
import com.asaf.appointment_service.service.notification.NotificationSettingsService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;
    private final SubscriptionService subscriptionService;
    private final AuthenticatedBusinessService authBusinessService;

    public NotificationSettingsController(NotificationSettingsService settingsService,
                                           SubscriptionService subscriptionService,
                                           AuthenticatedBusinessService authBusinessService) {
        this.settingsService = settingsService;
        this.subscriptionService = subscriptionService;
        this.authBusinessService = authBusinessService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getSettings() {
        Long businessId = authBusinessService.getCurrentBusinessId();
        NotificationSettings settings = settingsService.getSettings(businessId);
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        
        NotificationSettingsResponse response = toResponse(settings, plan, businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateSettings(
            @RequestBody UpdateNotificationSettingsRequest request) {
        Long businessId = authBusinessService.getCurrentBusinessId();
        
        NotificationSettings settings = settingsService.updateAllSettings(
                businessId,
                request.whatsappEnabled(),
                request.whatsappReminderEnabled(),
                request.whatsappConfirmationEnabled(),
                request.whatsappCancellationEnabled(),
                request.smsEnabled(),
                request.smsFallbackEnabled(),
                request.reminderHoursBefore(),
                request.secondReminderEnabled(),
                request.secondReminderHoursBefore()
        );
        
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        NotificationSettingsResponse response = toResponse(settings, plan, businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<QuotaResponse>> getQuota() {
        Long businessId = authBusinessService.getCurrentBusinessId();
        NotificationSettings settings = settingsService.getSettings(businessId);
        Plan plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        
        QuotaResponse response = new QuotaResponse(
                settings.getWhatsappUsedThisMonth(),
                plan.getMaxWhatsappMonthly(),
                settingsService.getRemainingWhatsappQuota(businessId),
                settings.getSmsUsedThisMonth(),
                plan.getMaxSmsMonthly(),
                settingsService.getRemainingSmsQuota(businessId)
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private NotificationSettingsResponse toResponse(NotificationSettings settings, Plan plan, Long businessId) {
        return new NotificationSettingsResponse(
                settings.isWhatsappEnabled(),
                settings.isWhatsappReminderEnabled(),
                settings.isWhatsappConfirmationEnabled(),
                settings.isWhatsappCancellationEnabled(),
                settings.isSmsEnabled(),
                settings.isSmsFallbackEnabled(),
                settings.getReminderHoursBefore(),
                settings.isSecondReminderEnabled(),
                settings.getSecondReminderHoursBefore(),
                // Plan bilgileri
                plan.hasFeature("WHATSAPP_NOTIFICATIONS"),
                plan.hasFeature("SMS_NOTIFICATIONS"),
                plan.getMaxWhatsappMonthly(),
                plan.getMaxSmsMonthly(),
                // Kullanım bilgileri
                settings.getWhatsappUsedThisMonth(),
                settings.getSmsUsedThisMonth(),
                settingsService.getRemainingWhatsappQuota(businessId),
                settingsService.getRemainingSmsQuota(businessId)
        );
    }

    public record UpdateNotificationSettingsRequest(
            boolean whatsappEnabled,
            boolean whatsappReminderEnabled,
            boolean whatsappConfirmationEnabled,
            boolean whatsappCancellationEnabled,
            boolean smsEnabled,
            boolean smsFallbackEnabled,
            int reminderHoursBefore,
            boolean secondReminderEnabled,
            Integer secondReminderHoursBefore
    ) {}

    public record NotificationSettingsResponse(
            boolean whatsappEnabled,
            boolean whatsappReminderEnabled,
            boolean whatsappConfirmationEnabled,
            boolean whatsappCancellationEnabled,
            boolean smsEnabled,
            boolean smsFallbackEnabled,
            int reminderHoursBefore,
            boolean secondReminderEnabled,
            Integer secondReminderHoursBefore,
            // Plan özellikleri
            boolean whatsappAvailable,
            boolean smsAvailable,
            int whatsappLimit,
            int smsLimit,
            // Kullanım
            int whatsappUsed,
            int smsUsed,
            int whatsappRemaining,
            int smsRemaining
    ) {}

    public record QuotaResponse(
            int whatsappUsed,
            int whatsappLimit,
            int whatsappRemaining,
            int smsUsed,
            int smsLimit,
            int smsRemaining
    ) {}
}
