package com.asaf.appointment_service.entity.plan;

import com.asaf.appointment_service.common.entity.BaseEntity;
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

    protected BusinessSubscription() {}

    private BusinessSubscription(Builder builder) {
        this.businessId = builder.businessId;
        this.planId = builder.planId;
        this.status = builder.status;
        this.trialEndDate = builder.trialEndDate;
        this.currentPeriodStart = builder.currentPeriodStart;
        this.currentPeriodEnd = builder.currentPeriodEnd;
        this.manual = builder.manual;
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

    public static class Builder {
        private Long businessId;
        private Long planId;
        private SubscriptionStatus status = SubscriptionStatus.TRIAL;
        private LocalDate trialEndDate;
        private LocalDate currentPeriodStart;
        private LocalDate currentPeriodEnd;
        private boolean manual = true;

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
