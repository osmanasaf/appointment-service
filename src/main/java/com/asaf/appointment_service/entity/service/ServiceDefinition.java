package com.asaf.appointment_service.entity.service;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "service_definitions")
public class ServiceDefinition extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "buffer_before_minutes")
    private int bufferBeforeMinutes = 0;

    @Column(name = "buffer_after_minutes")
    private int bufferAfterMinutes = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "currency")
    private String currency = "TRY";

    @Column(name = "requires_confirmation")
    private boolean requiresConfirmation = false;

    @Column(name = "same_day_booking_allowed")
    private boolean sameDayBookingAllowed = true;

    @Column(name = "package_eligible")
    private boolean packageEligible = true;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "display_order")
    private int displayOrder = 0;

    protected ServiceDefinition() {}

    private ServiceDefinition(Builder builder) {
        this.businessId = builder.businessId;
        this.name = builder.name;
        this.description = builder.description;
        this.durationMinutes = builder.durationMinutes;
        this.bufferBeforeMinutes = builder.bufferBeforeMinutes;
        this.bufferAfterMinutes = builder.bufferAfterMinutes;
        this.price = builder.price;
        this.currency = builder.currency;
        this.requiresConfirmation = builder.requiresConfirmation;
        this.sameDayBookingAllowed = builder.sameDayBookingAllowed;
        this.packageEligible = builder.packageEligible;
        this.active = builder.active;
        this.displayOrder = builder.displayOrder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int totalDurationMinutes() {
        return bufferBeforeMinutes + durationMinutes + bufferAfterMinutes;
    }

    public void updateDetails(String name, String description, int durationMinutes, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
    }

    public void updateBuffers(int before, int after) {
        this.bufferBeforeMinutes = before;
        this.bufferAfterMinutes = after;
    }

    public void updateFlags(boolean requiresConfirmation, boolean sameDayBookingAllowed, boolean packageEligible) {
        this.requiresConfirmation = requiresConfirmation;
        this.sameDayBookingAllowed = sameDayBookingAllowed;
        this.packageEligible = packageEligible;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getBufferBeforeMinutes() {
        return bufferBeforeMinutes;
    }

    public int getBufferAfterMinutes() {
        return bufferAfterMinutes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public boolean isSameDayBookingAllowed() {
        return sameDayBookingAllowed;
    }

    public boolean isPackageEligible() {
        return packageEligible;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public static class Builder {
        private Long businessId;
        private String name;
        private String description;
        private int durationMinutes;
        private int bufferBeforeMinutes = 0;
        private int bufferAfterMinutes = 0;
        private BigDecimal price;
        private String currency = "TRY";
        private boolean requiresConfirmation = false;
        private boolean sameDayBookingAllowed = true;
        private boolean packageEligible = true;
        private boolean active = true;
        private int displayOrder = 0;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder durationMinutes(int minutes) {
            this.durationMinutes = minutes;
            return this;
        }

        public Builder buffers(int before, int after) {
            this.bufferBeforeMinutes = before;
            this.bufferAfterMinutes = after;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder requiresConfirmation(boolean requires) {
            this.requiresConfirmation = requires;
            return this;
        }

        public Builder sameDayBookingAllowed(boolean allowed) {
            this.sameDayBookingAllowed = allowed;
            return this;
        }

        public Builder packageEligible(boolean eligible) {
            this.packageEligible = eligible;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder displayOrder(int order) {
            this.displayOrder = order;
            return this;
        }

        public ServiceDefinition build() {
            if (businessId == null) {
                throw new IllegalArgumentException("İşletme ID zorunludur");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Hizmet adı zorunludur");
            }
            if (durationMinutes < 15 || durationMinutes > 360) {
                throw new IllegalArgumentException("Hizmet süresi 15-360 dakika arasında olmalıdır");
            }
            return new ServiceDefinition(this);
        }
    }
}
