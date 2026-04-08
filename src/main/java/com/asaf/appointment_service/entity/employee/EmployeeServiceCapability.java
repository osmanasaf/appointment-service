package com.asaf.appointment_service.entity.employee;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
    name = "employee_service_capabilities",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_esc_employee_service",
        columnNames = {"employee_id", "service_id"}
    )
)
public class EmployeeServiceCapability extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "custom_duration_minutes")
    private Integer customDurationMinutes;

    @Column(name = "custom_price", precision = 10, scale = 2)
    private BigDecimal customPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false)
    private SkillLevel skillLevel = SkillLevel.INTERMEDIATE;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected EmployeeServiceCapability() {}

    private EmployeeServiceCapability(Builder builder) {
        this.employeeId = builder.employeeId;
        this.serviceId = builder.serviceId;
        this.customDurationMinutes = builder.customDurationMinutes;
        this.customPrice = builder.customPrice;
        this.skillLevel = builder.skillLevel;
        this.active = builder.active;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int effectiveDuration(int serviceDefaultDuration) {
        return customDurationMinutes != null ? customDurationMinutes : serviceDefaultDuration;
    }

    public BigDecimal effectivePrice(BigDecimal serviceDefaultPrice) {
        return customPrice != null ? customPrice : serviceDefaultPrice;
    }

    public void updateDetails(Integer newCustomDuration, BigDecimal newCustomPrice, SkillLevel newSkillLevel) {
        this.customDurationMinutes = newCustomDuration;
        this.customPrice = newCustomPrice;
        if (newSkillLevel != null) {
            this.skillLevel = newSkillLevel;
        }
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public Integer getCustomDurationMinutes() {
        return customDurationMinutes;
    }

    public BigDecimal getCustomPrice() {
        return customPrice;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public boolean isActive() {
        return active;
    }

    public static class Builder {
        private Long employeeId;
        private Long serviceId;
        private Integer customDurationMinutes;
        private BigDecimal customPrice;
        private SkillLevel skillLevel = SkillLevel.INTERMEDIATE;
        private boolean active = true;

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder serviceId(Long serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder customDurationMinutes(Integer minutes) {
            this.customDurationMinutes = minutes;
            return this;
        }

        public Builder customPrice(BigDecimal price) {
            this.customPrice = price;
            return this;
        }

        public Builder skillLevel(SkillLevel level) {
            this.skillLevel = level;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public EmployeeServiceCapability build() {
            Objects.requireNonNull(employeeId, "Employee ID zorunludur");
            Objects.requireNonNull(serviceId, "Service ID zorunludur");
            return new EmployeeServiceCapability(this);
        }
    }
}
