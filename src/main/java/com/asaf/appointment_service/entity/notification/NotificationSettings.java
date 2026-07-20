package com.asaf.appointment_service.entity.notification;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * İşletme bazlı bildirim ayarları.
 * Her işletme kendi bildirim tercihlerini yönetebilir.
 */
@Entity
@Table(name = "notification_settings")
public class NotificationSettings extends BaseEntity {

    @Column(name = "business_id", nullable = false, unique = true)
    private Long businessId;

    // WhatsApp ayarları
    @Column(name = "whatsapp_enabled", nullable = false)
    private boolean whatsappEnabled = true;

    @Column(name = "whatsapp_reminder_enabled", nullable = false)
    private boolean whatsappReminderEnabled = true;

    @Column(name = "whatsapp_confirmation_enabled", nullable = false)
    private boolean whatsappConfirmationEnabled = true;

    @Column(name = "whatsapp_cancellation_enabled", nullable = false)
    private boolean whatsappCancellationEnabled = true;

    // SMS ayarları
    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = false;

    @Column(name = "sms_fallback_enabled", nullable = false)
    private boolean smsFallbackEnabled = false;

    // Hatırlatma ayarları
    @Column(name = "reminder_hours_before", nullable = false)
    private int reminderHoursBefore = 24;

    @Column(name = "second_reminder_enabled", nullable = false)
    private boolean secondReminderEnabled = false;

    @Column(name = "second_reminder_hours_before")
    private Integer secondReminderHoursBefore = 2;

    // Değerlendirme (review) isteği ayarları
    @Column(name = "review_request_enabled", nullable = false)
    private boolean reviewRequestEnabled = true;

    @Column(name = "review_request_delay_hours", nullable = false)
    private int reviewRequestDelayHours = 2;

    // Kullanım sayaçları (aylık reset)
    @Column(name = "whatsapp_used_this_month", nullable = false)
    private int whatsappUsedThisMonth = 0;

    @Column(name = "sms_used_this_month", nullable = false)
    private int smsUsedThisMonth = 0;

    @Column(name = "usage_reset_month")
    private Integer usageResetMonth;

    @Column(name = "usage_reset_year")
    private Integer usageResetYear;

    protected NotificationSettings() {}

    private NotificationSettings(Builder builder) {
        this.businessId = builder.businessId;
        this.whatsappEnabled = builder.whatsappEnabled;
        this.whatsappReminderEnabled = builder.whatsappReminderEnabled;
        this.whatsappConfirmationEnabled = builder.whatsappConfirmationEnabled;
        this.whatsappCancellationEnabled = builder.whatsappCancellationEnabled;
        this.smsEnabled = builder.smsEnabled;
        this.smsFallbackEnabled = builder.smsFallbackEnabled;
        this.reminderHoursBefore = builder.reminderHoursBefore;
        this.secondReminderEnabled = builder.secondReminderEnabled;
        this.secondReminderHoursBefore = builder.secondReminderHoursBefore;
        this.reviewRequestEnabled = builder.reviewRequestEnabled;
        this.reviewRequestDelayHours = builder.reviewRequestDelayHours;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static NotificationSettings createDefault(Long businessId) {
        return builder().businessId(businessId).build();
    }

    // Update methods
    public void updateWhatsAppSettings(boolean enabled, boolean reminderEnabled, 
                                        boolean confirmationEnabled, boolean cancellationEnabled) {
        this.whatsappEnabled = enabled;
        this.whatsappReminderEnabled = reminderEnabled;
        this.whatsappConfirmationEnabled = confirmationEnabled;
        this.whatsappCancellationEnabled = cancellationEnabled;
    }

    public void updateSmsSettings(boolean enabled, boolean fallbackEnabled) {
        this.smsEnabled = enabled;
        this.smsFallbackEnabled = fallbackEnabled;
    }

    public void updateReminderSettings(int hoursBefore, boolean secondEnabled, Integer secondHoursBefore) {
        this.reminderHoursBefore = hoursBefore;
        this.secondReminderEnabled = secondEnabled;
        this.secondReminderHoursBefore = secondHoursBefore;
    }

    public void updateReviewRequestSettings(boolean enabled, int delayHours) {
        this.reviewRequestEnabled = enabled;
        this.reviewRequestDelayHours = delayHours;
    }

    public void incrementWhatsappUsage() {
        this.whatsappUsedThisMonth++;
    }

    public void incrementSmsUsage() {
        this.smsUsedThisMonth++;
    }

    public void resetMonthlyUsage(int month, int year) {
        this.whatsappUsedThisMonth = 0;
        this.smsUsedThisMonth = 0;
        this.usageResetMonth = month;
        this.usageResetYear = year;
    }

    // Getters
    public Long getBusinessId() {
        return businessId;
    }

    public boolean isWhatsappEnabled() {
        return whatsappEnabled;
    }

    public boolean isWhatsappReminderEnabled() {
        return whatsappReminderEnabled;
    }

    public boolean isWhatsappConfirmationEnabled() {
        return whatsappConfirmationEnabled;
    }

    public boolean isWhatsappCancellationEnabled() {
        return whatsappCancellationEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public boolean isSmsFallbackEnabled() {
        return smsFallbackEnabled;
    }

    public int getReminderHoursBefore() {
        return reminderHoursBefore;
    }

    public boolean isSecondReminderEnabled() {
        return secondReminderEnabled;
    }

    public Integer getSecondReminderHoursBefore() {
        return secondReminderHoursBefore;
    }

    public boolean isReviewRequestEnabled() {
        return reviewRequestEnabled;
    }

    public int getReviewRequestDelayHours() {
        return reviewRequestDelayHours;
    }

    public int getWhatsappUsedThisMonth() {
        return whatsappUsedThisMonth;
    }

    public int getSmsUsedThisMonth() {
        return smsUsedThisMonth;
    }

    public Integer getUsageResetMonth() {
        return usageResetMonth;
    }

    public Integer getUsageResetYear() {
        return usageResetYear;
    }

    public static class Builder {
        private Long businessId;
        private boolean whatsappEnabled = true;
        private boolean whatsappReminderEnabled = true;
        private boolean whatsappConfirmationEnabled = true;
        private boolean whatsappCancellationEnabled = true;
        private boolean smsEnabled = false;
        private boolean smsFallbackEnabled = false;
        private int reminderHoursBefore = 24;
        private boolean secondReminderEnabled = false;
        private Integer secondReminderHoursBefore = 2;
        private boolean reviewRequestEnabled = true;
        private int reviewRequestDelayHours = 2;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder whatsappEnabled(boolean whatsappEnabled) {
            this.whatsappEnabled = whatsappEnabled;
            return this;
        }

        public Builder whatsappReminderEnabled(boolean whatsappReminderEnabled) {
            this.whatsappReminderEnabled = whatsappReminderEnabled;
            return this;
        }

        public Builder whatsappConfirmationEnabled(boolean whatsappConfirmationEnabled) {
            this.whatsappConfirmationEnabled = whatsappConfirmationEnabled;
            return this;
        }

        public Builder whatsappCancellationEnabled(boolean whatsappCancellationEnabled) {
            this.whatsappCancellationEnabled = whatsappCancellationEnabled;
            return this;
        }

        public Builder smsEnabled(boolean smsEnabled) {
            this.smsEnabled = smsEnabled;
            return this;
        }

        public Builder smsFallbackEnabled(boolean smsFallbackEnabled) {
            this.smsFallbackEnabled = smsFallbackEnabled;
            return this;
        }

        public Builder reminderHoursBefore(int reminderHoursBefore) {
            this.reminderHoursBefore = reminderHoursBefore;
            return this;
        }

        public Builder secondReminderEnabled(boolean secondReminderEnabled) {
            this.secondReminderEnabled = secondReminderEnabled;
            return this;
        }

        public Builder secondReminderHoursBefore(Integer secondReminderHoursBefore) {
            this.secondReminderHoursBefore = secondReminderHoursBefore;
            return this;
        }

        public Builder reviewRequestEnabled(boolean reviewRequestEnabled) {
            this.reviewRequestEnabled = reviewRequestEnabled;
            return this;
        }

        public Builder reviewRequestDelayHours(int reviewRequestDelayHours) {
            this.reviewRequestDelayHours = reviewRequestDelayHours;
            return this;
        }

        public NotificationSettings build() {
            if (businessId == null) {
                throw new IllegalArgumentException("Business ID zorunludur");
            }
            return new NotificationSettings(this);
        }
    }
}
