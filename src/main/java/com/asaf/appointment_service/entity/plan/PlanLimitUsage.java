package com.asaf.appointment_service.entity.plan;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan_limit_usages")
public class PlanLimitUsage extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "usage_year", nullable = false)
    private int year;

    @Column(name = "usage_month", nullable = false)
    private int month;

    @Column(name = "appointments_count", nullable = false)
    private int appointmentsCount;

    @Column(name = "sms_sent", nullable = false)
    private int smsSent;

    @Column(name = "whatsapp_sent", nullable = false)
    private int whatsappSent;

    protected PlanLimitUsage() {}

    private PlanLimitUsage(Builder builder) {
        this.businessId = builder.businessId;
        this.year = builder.year;
        this.month = builder.month;
        this.appointmentsCount = builder.appointmentsCount;
        this.smsSent = builder.smsSent;
        this.whatsappSent = builder.whatsappSent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void incrementAppointments() {
        this.appointmentsCount++;
    }

    public void incrementSms() {
        this.smsSent++;
    }

    public void incrementWhatsapp() {
        this.whatsappSent++;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getAppointmentsCount() {
        return appointmentsCount;
    }

    public int getSmsSent() {
        return smsSent;
    }

    public int getWhatsappSent() {
        return whatsappSent;
    }

    public static class Builder {
        private Long businessId;
        private int year;
        private int month;
        private int appointmentsCount;
        private int smsSent;
        private int whatsappSent;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder year(int year) {
            this.year = year;
            return this;
        }

        public Builder month(int month) {
            this.month = month;
            return this;
        }

        public Builder appointmentsCount(int appointmentsCount) {
            this.appointmentsCount = appointmentsCount;
            return this;
        }

        public Builder smsSent(int smsSent) {
            this.smsSent = smsSent;
            return this;
        }

        public Builder whatsappSent(int whatsappSent) {
            this.whatsappSent = whatsappSent;
            return this;
        }

        public PlanLimitUsage build() {
            if (businessId == null) {
                throw new IllegalArgumentException("Business ID zorunludur");
            }
            if (year <= 0 || month <= 0 || month > 12) {
                throw new IllegalArgumentException("Kullanım dönemi geçersiz");
            }
            return new PlanLimitUsage(this);
        }
    }
}
