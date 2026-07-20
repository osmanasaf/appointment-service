package com.asaf.appointment_service.entity.business;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "businesses")
public class Business extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(name = "phone_country_code")
    private String phoneCountryCode;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "auto_confirm")
    private boolean autoConfirm = true;

    @Column(name = "reminder_hours_before")
    private int reminderHoursBefore = 24;

    @Column(name = "same_day_booking_allowed")
    private boolean sameDayBookingAllowed = true;

    @Column(name = "max_advance_booking_days")
    private int maxAdvanceBookingDays = 30;

    /** Aynı müşterinin aynı güne alabileceği en fazla randevu sayısı. null = sınırsız. */
    @Column(name = "max_appointments_per_customer_per_day")
    private Integer maxAppointmentsPerCustomerPerDay;

    @Column(name = "whatsapp_send_price")
    private boolean whatsappSendPrice = false;

    @Column(name = "google_review_url", length = 500)
    private String googleReviewUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "tiktok_url", length = 500)
    private String tiktokUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessStatus status = BusinessStatus.ACTIVE;

    protected Business() {}

    private Business(Builder builder) {
        if (builder.id != null) this.setId(builder.id);
        this.name = builder.name;
        this.slug = builder.slug;
        this.phoneCountryCode = builder.phoneCountryCode;
        this.phoneNumber = builder.phoneNumber;
        this.email = builder.email;
        this.address = builder.address;
        this.description = builder.description;
        this.logoUrl = builder.logoUrl;
        this.autoConfirm = builder.autoConfirm;
        this.reminderHoursBefore = builder.reminderHoursBefore;
        this.sameDayBookingAllowed = builder.sameDayBookingAllowed;
        this.maxAdvanceBookingDays = builder.maxAdvanceBookingDays;
        this.maxAppointmentsPerCustomerPerDay = builder.maxAppointmentsPerCustomerPerDay;
        this.whatsappSendPrice = builder.whatsappSendPrice;
        this.googleReviewUrl = builder.googleReviewUrl;
        this.instagramUrl = builder.instagramUrl;
        this.tiktokUrl = builder.tiktokUrl;
        this.facebookUrl = builder.facebookUrl;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.category = builder.category;
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateProfile(String name, String email, String address, String description, String logoUrl) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.description = description;
        this.logoUrl = logoUrl;
    }

    public void updateCategory(BusinessCategory category) {
        if (category != null) {
            this.category = category;
        }
    }

    public void updateSettings(boolean autoConfirm, int reminderHoursBefore,
                               boolean sameDayBookingAllowed, int maxAdvanceBookingDays,
                               Integer maxAppointmentsPerCustomerPerDay,
                               Boolean whatsappSendPrice) {
        this.autoConfirm = autoConfirm;
        this.reminderHoursBefore = reminderHoursBefore;
        this.sameDayBookingAllowed = sameDayBookingAllowed;
        this.maxAdvanceBookingDays = maxAdvanceBookingDays;
        this.maxAppointmentsPerCustomerPerDay = maxAppointmentsPerCustomerPerDay;
        if (whatsappSendPrice != null) {
            this.whatsappSendPrice = whatsappSendPrice;
        }
    }

    public void updateGoogleReviewUrl(String googleReviewUrl) {
        this.googleReviewUrl = googleReviewUrl;
    }

    public void updateSocialLinks(String instagramUrl, String tiktokUrl, String facebookUrl) {
        this.instagramUrl = instagramUrl;
        this.tiktokUrl = tiktokUrl;
        this.facebookUrl = facebookUrl;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void activate() {
        this.status = BusinessStatus.ACTIVE;
    }

    public void suspend() {
        this.status = BusinessStatus.SUSPENDED;
    }

    public boolean isActive() {
        return this.status == BusinessStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFormattedPhoneNumber() {
        if (phoneNumber == null) return null;
        String code = phoneCountryCode != null ? phoneCountryCode : "+90";
        return code + phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isAutoConfirm() {
        return autoConfirm;
    }

    public int getReminderHoursBefore() {
        return reminderHoursBefore;
    }

    public boolean isSameDayBookingAllowed() {
        return sameDayBookingAllowed;
    }

    public int getMaxAdvanceBookingDays() {
        return maxAdvanceBookingDays;
    }

    public Integer getMaxAppointmentsPerCustomerPerDay() {
        return maxAppointmentsPerCustomerPerDay;
    }

    public boolean isWhatsappSendPrice() {
        return whatsappSendPrice;
    }

    public String getGoogleReviewUrl() {
        return googleReviewUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public String getTiktokUrl() {
        return tiktokUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public BusinessCategory getCategory() {
        return category;
    }

    public BusinessStatus getStatus() {
        return status;
    }

    public static class Builder {
        private Long id;
        private String name;
        private String slug;
        private String phoneCountryCode = "+90";
        private String phoneNumber;
        private String email;
        private String address;
        private String description;
        private String logoUrl;
        private boolean autoConfirm = true;
        private int reminderHoursBefore = 24;
        private boolean sameDayBookingAllowed = true;
        private int maxAdvanceBookingDays = 30;
        private Integer maxAppointmentsPerCustomerPerDay;
        private boolean whatsappSendPrice = false;
        private String googleReviewUrl;
        private String instagramUrl;
        private String tiktokUrl;
        private String facebookUrl;
        private Double latitude;
        private Double longitude;
        private BusinessCategory category;
        private BusinessStatus status = BusinessStatus.ACTIVE;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder phone(String countryCode, String number) {
            this.phoneCountryCode = countryCode;
            this.phoneNumber = number;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public Builder autoConfirm(boolean autoConfirm) {
            this.autoConfirm = autoConfirm;
            return this;
        }

        public Builder reminderHoursBefore(int hours) {
            this.reminderHoursBefore = hours;
            return this;
        }

        public Builder sameDayBookingAllowed(boolean allowed) {
            this.sameDayBookingAllowed = allowed;
            return this;
        }

        public Builder maxAdvanceBookingDays(int days) {
            this.maxAdvanceBookingDays = days;
            return this;
        }

        public Builder maxAppointmentsPerCustomerPerDay(Integer max) {
            this.maxAppointmentsPerCustomerPerDay = max;
            return this;
        }

        public Builder whatsappSendPrice(boolean sendPrice) {
            this.whatsappSendPrice = sendPrice;
            return this;
        }

        public Builder googleReviewUrl(String googleReviewUrl) {
            this.googleReviewUrl = googleReviewUrl;
            return this;
        }

        public Builder instagramUrl(String instagramUrl) {
            this.instagramUrl = instagramUrl;
            return this;
        }

        public Builder tiktokUrl(String tiktokUrl) {
            this.tiktokUrl = tiktokUrl;
            return this;
        }

        public Builder facebookUrl(String facebookUrl) {
            this.facebookUrl = facebookUrl;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder category(BusinessCategory category) {
            this.category = category;
            return this;
        }

        public Builder status(BusinessStatus status) {
            this.status = status;
            return this;
        }

        public Business build() {
            if (id == null) {
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("İşletme adı zorunludur");
                }
                if (slug == null || slug.isBlank()) {
                    throw new IllegalArgumentException("İşletme slug zorunludur");
                }
                if (category == null) {
                    throw new IllegalArgumentException("İşletme kategorisi zorunludur");
                }
            }
            return new Business(this);
        }
    }
}
