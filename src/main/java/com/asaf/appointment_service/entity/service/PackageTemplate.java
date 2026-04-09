package com.asaf.appointment_service.entity.service;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "package_templates")
public class PackageTemplate extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected PackageTemplate() {}

    private PackageTemplate(Builder builder) {
        if (builder.id != null) this.setId(builder.id);
        this.businessId = builder.businessId;
        this.serviceId = builder.serviceId;
        this.name = builder.name;
        this.totalSessions = builder.totalSessions;
        this.price = builder.price;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getName() {
        return name;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public void update(String name, Integer totalSessions, BigDecimal price) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (totalSessions != null && totalSessions > 0) {
            this.totalSessions = totalSessions;
        }
        this.price = price;
    }

    public static class Builder {
        private Long id;
        private Long businessId;
        private Long serviceId;
        private String name;
        private int totalSessions;
        private BigDecimal price;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder serviceId(Long serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder totalSessions(int totalSessions) {
            this.totalSessions = totalSessions;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public PackageTemplate build() {
            if (id == null) {
                if (businessId == null) {
                    throw new IllegalArgumentException("İşletme ID zorunludur");
                }
                if (serviceId == null) {
                    throw new IllegalArgumentException("Hizmet ID zorunludur");
                }
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("Paket adı zorunludur");
                }
                if (totalSessions <= 0) {
                    throw new IllegalArgumentException("Seans sayısı pozitif olmalıdır");
                }
            }
            return new PackageTemplate(this);
        }
    }
}
