package com.asaf.appointment_service.entity.plan;

import com.asaf.appointment_service.common.entity.BaseEntity;
import com.asaf.appointment_service.common.persistence.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PlanCode code;

    @Column(name = "max_employees", nullable = false)
    private int maxEmployees;

    @Column(name = "max_appointments_monthly", nullable = false)
    private int maxAppointmentsMonthly;

    @Column(name = "max_services", nullable = false)
    private int maxServices;

    @Column(name = "max_branches", nullable = false)
    private int maxBranches;

    @Column(name = "max_whatsapp_monthly", nullable = false)
    private int maxWhatsappMonthly;

    @Column(name = "max_sms_monthly", nullable = false)
    private int maxSmsMonthly;

    @Convert(converter = StringListJsonConverter.class)
    @Column(nullable = false, length = 4000)
    private List<String> features;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal yearlyPrice;

    protected Plan() {}

    private Plan(Builder builder) {
        this.code = builder.code;
        this.maxEmployees = builder.maxEmployees;
        this.maxAppointmentsMonthly = builder.maxAppointmentsMonthly;
        this.maxServices = builder.maxServices;
        this.maxBranches = builder.maxBranches;
        this.maxWhatsappMonthly = builder.maxWhatsappMonthly;
        this.maxSmsMonthly = builder.maxSmsMonthly;
        this.features = builder.features;
        this.monthlyPrice = builder.monthlyPrice;
        this.yearlyPrice = builder.yearlyPrice;
    }

    public static Builder builder() {
        return new Builder();
    }

    public PlanCode getCode() {
        return code;
    }

    public int getMaxEmployees() {
        return maxEmployees;
    }

    public int getMaxAppointmentsMonthly() {
        return maxAppointmentsMonthly;
    }

    public int getMaxServices() {
        return maxServices;
    }

    public int getMaxBranches() {
        return maxBranches;
    }

    public int getMaxWhatsappMonthly() {
        return maxWhatsappMonthly;
    }

    public int getMaxSmsMonthly() {
        return maxSmsMonthly;
    }

    public boolean isUnlimitedWhatsapp() {
        return maxWhatsappMonthly < 0;
    }

    public boolean isUnlimitedSms() {
        return maxSmsMonthly < 0;
    }

    public List<String> getFeatures() {
        return features;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public BigDecimal getYearlyPrice() {
        return yearlyPrice;
    }

    public boolean isUnlimitedEmployees() {
        return maxEmployees < 0;
    }

    public boolean isUnlimitedAppointments() {
        return maxAppointmentsMonthly < 0;
    }

    public boolean isUnlimitedServices() {
        return maxServices < 0;
    }

    public boolean isUnlimitedBranches() {
        return maxBranches < 0;
    }

    public boolean hasFeature(String featureCode) {
        return features != null && features.contains(featureCode);
    }

    /**
     * Eksik özellik kodlarını mevcut listeye ekler; zaten var olanları yeniden eklemez.
     * @return değişiklik yapıldıysa true (persist edilmeli)
     */
    public boolean mergeFeatures(List<String> additionalFeatureCodes) {
        List<String> missing = additionalFeatureCodes.stream()
            .distinct()
            .filter(featureCode -> !hasFeature(featureCode))
            .toList();
        if (missing.isEmpty()) {
            return false;
        }
        List<String> merged = new ArrayList<>(features);
        merged.addAll(missing);
        this.features = List.copyOf(merged);
        return true;
    }

    public static class Builder {
        private PlanCode code;
        private int maxEmployees;
        private int maxAppointmentsMonthly;
        private int maxServices;
        private int maxBranches;
        private int maxWhatsappMonthly;
        private int maxSmsMonthly;
        private List<String> features = List.of();
        private BigDecimal monthlyPrice;
        private BigDecimal yearlyPrice;

        public Builder code(PlanCode code) {
            this.code = code;
            return this;
        }

        public Builder maxEmployees(int maxEmployees) {
            this.maxEmployees = maxEmployees;
            return this;
        }

        public Builder maxAppointmentsMonthly(int maxAppointmentsMonthly) {
            this.maxAppointmentsMonthly = maxAppointmentsMonthly;
            return this;
        }

        public Builder maxServices(int maxServices) {
            this.maxServices = maxServices;
            return this;
        }

        public Builder maxBranches(int maxBranches) {
            this.maxBranches = maxBranches;
            return this;
        }

        public Builder maxWhatsappMonthly(int maxWhatsappMonthly) {
            this.maxWhatsappMonthly = maxWhatsappMonthly;
            return this;
        }

        public Builder maxSmsMonthly(int maxSmsMonthly) {
            this.maxSmsMonthly = maxSmsMonthly;
            return this;
        }

        public Builder features(List<String> features) {
            this.features = features;
            return this;
        }

        public Builder monthlyPrice(BigDecimal monthlyPrice) {
            this.monthlyPrice = monthlyPrice;
            return this;
        }

        public Builder yearlyPrice(BigDecimal yearlyPrice) {
            this.yearlyPrice = yearlyPrice;
            return this;
        }

        public Plan build() {
            if (code == null) {
                throw new IllegalArgumentException("Plan kodu zorunludur");
            }
            if (monthlyPrice == null || yearlyPrice == null) {
                throw new IllegalArgumentException("Plan fiyatları zorunludur");
            }
            return new Plan(this);
        }
    }
}
