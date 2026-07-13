package com.asaf.appointment_service.entity.plan;

import com.asaf.appointment_service.common.entity.BaseEntity;
import com.asaf.appointment_service.entity.payment.BillingPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "business_subscriptions")
public class BusinessSubscription extends BaseEntity {

    @Column(name = "business_id", nullable = false, unique = true)
    private Long businessId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "current_period_start", nullable = false)
    private LocalDate currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDate currentPeriodEnd;

    @Column(nullable = false)
    private boolean manual;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period")
    private BillingPeriod billingPeriod;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = true;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    /**
     * AES-GCM ile şifrelenmiş PayTR utoken (işletme/kart sahibi tokenı).
     * Ham değeri asla loglamayın; sadece EncryptionService üzerinden erişin.
     */
    @Column(name = "paytr_utoken_encrypted", length = 512)
    private String paytrUtokenEncrypted;

    /**
     * Dönem bitişine kaç gün kala son yenileme hatırlatması gönderildiğini tutar
     * (7/3/1). Aynı eşik için mükerrer gönderimi engellemek için kullanılır.
     */
    @Column(name = "last_reminder_days_before")
    private Integer lastReminderDaysBefore;

    protected BusinessSubscription() {}

    private BusinessSubscription(Builder builder) {
        this.businessId = builder.businessId;
        this.planId = builder.planId;
        this.status = builder.status;
        this.trialEndDate = builder.trialEndDate;
        this.currentPeriodStart = builder.currentPeriodStart;
        this.currentPeriodEnd = builder.currentPeriodEnd;
        this.manual = builder.manual;
        this.billingPeriod = builder.billingPeriod;
        this.autoRenew = builder.autoRenew;
        this.nextPaymentDate = builder.nextPaymentDate;
        this.lastPaymentDate = builder.lastPaymentDate;
        this.paytrUtokenEncrypted = builder.paytrUtokenEncrypted;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void switchPlan(Long newPlanId, boolean manual, LocalDate periodStart, LocalDate periodEnd) {
        this.planId = newPlanId;
        this.status = SubscriptionStatus.ACTIVE;
        this.trialEndDate = null;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.manual = manual;
        this.lastReminderDaysBefore = null;
    }

    public void activatePayment(Long planId, BillingPeriod billingPeriod,
                                LocalDate periodStart, LocalDate periodEnd,
                                LocalDate nextPaymentDate, String paytrUtokenEncrypted) {
        this.planId = planId;
        this.status = SubscriptionStatus.ACTIVE;
        this.trialEndDate = null;
        this.manual = false;
        this.billingPeriod = billingPeriod;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.nextPaymentDate = nextPaymentDate;
        this.lastPaymentDate = periodStart;
        this.lastReminderDaysBefore = null;
        if (paytrUtokenEncrypted != null) {
            this.paytrUtokenEncrypted = paytrUtokenEncrypted;
        }
    }

    public void renewPayment(LocalDate periodStart, LocalDate periodEnd, LocalDate nextPaymentDate) {
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.lastPaymentDate = periodStart;
        this.nextPaymentDate = nextPaymentDate;
        this.lastReminderDaysBefore = null;
    }

    /**
     * Bir eşik için (7/3/1 gün kala) hatırlatma gönderildiğini işaretler; aynı eşik
     * için tekrar gönderimi engeller.
     */
    public void markReminderSent(int daysBefore) {
        this.lastReminderDaysBefore = daysBefore;
    }

    public boolean hasReminderBeenSentFor(int daysBefore) {
        return lastReminderDaysBefore != null && lastReminderDaysBefore == daysBefore;
    }

    /**
     * ACTIVE bir abonelik dönem bitişinden itibaren tolerans (grace) süresi kadar
     * ödeme yapılmadan geçerse süresi dolmuş sayılır.
     */
    public boolean isGraceExpired(LocalDate today, int gracePeriodDays) {
        return status == SubscriptionStatus.ACTIVE
            && currentPeriodEnd != null
            && currentPeriodEnd.plusDays(gracePeriodDays).isBefore(today);
    }

    public void cancelAutoRenew() {
        this.autoRenew = false;
        this.nextPaymentDate = null;
    }

    public void enableAutoRenew(LocalDate nextPaymentDate) {
        this.autoRenew = true;
        this.nextPaymentDate = nextPaymentDate;
    }

    public void startTrial(Long planId, LocalDate periodStart, LocalDate trialEndDate) {
        this.planId = planId;
        this.status = SubscriptionStatus.TRIAL;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = trialEndDate;
        this.trialEndDate = trialEndDate;
        this.manual = true;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
    }

    public boolean isTrialExpired(LocalDate today) {
        return status == SubscriptionStatus.TRIAL
            && trialEndDate != null
            && trialEndDate.isBefore(today);
    }

    public boolean isUsable() {
        return status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.ACTIVE;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getPlanId() {
        return planId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public LocalDate getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public LocalDate getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isManual() {
        return manual;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }

    public String getPaytrUtokenEncrypted() {
        return paytrUtokenEncrypted;
    }

    public boolean hasStoredPaymentMethod() {
        return paytrUtokenEncrypted != null && !paytrUtokenEncrypted.isBlank();
    }

    public Integer getLastReminderDaysBefore() {
        return lastReminderDaysBefore;
    }

    public static class Builder {
        private Long businessId;
        private Long planId;
        private SubscriptionStatus status = SubscriptionStatus.TRIAL;
        private LocalDate trialEndDate;
        private LocalDate currentPeriodStart;
        private LocalDate currentPeriodEnd;
        private boolean manual = true;
        private BillingPeriod billingPeriod;
        private boolean autoRenew = true;
        private LocalDate nextPaymentDate;
        private LocalDate lastPaymentDate;
        private String paytrUtokenEncrypted;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder planId(Long planId) {
            this.planId = planId;
            return this;
        }

        public Builder status(SubscriptionStatus status) {
            this.status = status;
            return this;
        }

        public Builder trialEndDate(LocalDate trialEndDate) {
            this.trialEndDate = trialEndDate;
            return this;
        }

        public Builder currentPeriodStart(LocalDate currentPeriodStart) {
            this.currentPeriodStart = currentPeriodStart;
            return this;
        }

        public Builder currentPeriodEnd(LocalDate currentPeriodEnd) {
            this.currentPeriodEnd = currentPeriodEnd;
            return this;
        }

        public Builder manual(boolean manual) {
            this.manual = manual;
            return this;
        }

        public Builder billingPeriod(BillingPeriod billingPeriod) {
            this.billingPeriod = billingPeriod;
            return this;
        }

        public Builder autoRenew(boolean autoRenew) {
            this.autoRenew = autoRenew;
            return this;
        }

        public Builder nextPaymentDate(LocalDate nextPaymentDate) {
            this.nextPaymentDate = nextPaymentDate;
            return this;
        }

        public Builder lastPaymentDate(LocalDate lastPaymentDate) {
            this.lastPaymentDate = lastPaymentDate;
            return this;
        }

        public Builder paytrUtokenEncrypted(String paytrUtokenEncrypted) {
            this.paytrUtokenEncrypted = paytrUtokenEncrypted;
            return this;
        }

        public BusinessSubscription build() {
            if (businessId == null || planId == null) {
                throw new IllegalArgumentException("Business ve Plan ID zorunludur");
            }
            if (currentPeriodStart == null || currentPeriodEnd == null) {
                throw new IllegalArgumentException("Abonelik dönem tarihleri zorunludur");
            }
            return new BusinessSubscription(this);
        }
    }
}
