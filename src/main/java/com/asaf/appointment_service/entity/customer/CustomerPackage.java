package com.asaf.appointment_service.entity.customer;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_packages")
public class CustomerPackage extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Column(name = "remaining_sessions", nullable = false)
    private int remainingSessions;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "template_id")
    private Long templateId;

    protected CustomerPackage() {}

    private CustomerPackage(Builder builder) {
        if (builder.id != null) this.setId(builder.id);
        this.customerId = builder.customerId;
        this.serviceId = builder.serviceId;
        this.businessId = builder.businessId;
        this.name = builder.name;
        this.totalSessions = builder.totalSessions;
        this.remainingSessions = builder.remainingSessions != null ? builder.remainingSessions : builder.totalSessions;
        this.expiresAt = builder.expiresAt;
        this.templateId = builder.templateId;
        if (builder.active != null) this.active = builder.active;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void deductSession() {
        if (remainingSessions <= 0) {
            throw new IllegalStateException("Pakette kalan seans bulunmuyor");
        }
        this.remainingSessions--;
        if (this.remainingSessions == 0) {
            this.active = false;
        }
    }

    public void addSessions(int count) {
        this.remainingSessions += count;
        this.totalSessions += count;
        this.active = true;
    }

    public void updateDetails(String name, LocalDateTime expiresAt, boolean active) {
        this.name = name;
        this.expiresAt = expiresAt;
        this.active = active && remainingSessions > 0 && !isExpired();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isLowOnSessions() {
        return remainingSessions <= 2 && remainingSessions > 0;
    }

    public boolean hasRemainingSessions() {
        return remainingSessions > 0 && !isExpired();
    }

    public void expire() {
        this.active = false;
    }

    public void markAsExpired() {
        this.active = false;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public int getRemainingSessions() {
        return remainingSessions;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public static class Builder {
        private Long id;
        private Long customerId;
        private Long serviceId;
        private Long businessId;
        private String name;
        private int totalSessions;
        private Integer remainingSessions;
        private Boolean active;
        private LocalDateTime expiresAt;
        private Long templateId;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder serviceId(Long serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sessions(int sessions) {
            this.totalSessions = sessions;
            return this;
        }

        public Builder totalSessions(int totalSessions) {
            this.totalSessions = totalSessions;
            return this;
        }

        public Builder remainingSessions(int remainingSessions) {
            this.remainingSessions = remainingSessions;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder expiresAt(LocalDateTime date) {
            this.expiresAt = date;
            return this;
        }

        public Builder templateId(Long templateId) {
            this.templateId = templateId;
            return this;
        }

        public CustomerPackage build() {
            if (id == null) {
                if (customerId == null) {
                    throw new IllegalArgumentException("Müşteri ID zorunludur");
                }
                if (serviceId == null) {
                    throw new IllegalArgumentException("Hizmet ID zorunludur");
                }
                if (totalSessions <= 0) {
                    throw new IllegalArgumentException("Seans sayısı pozitif olmalıdır");
                }
            }
            return new CustomerPackage(this);
        }
    }
}
