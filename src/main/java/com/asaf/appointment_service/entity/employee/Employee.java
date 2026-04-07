package com.asaf.appointment_service.entity.employee;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_country_code")
    private String phoneCountryCode;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String email;

    @Column(length = 500)
    private String bio;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "is_owner")
    private boolean owner = false;

    protected Employee() {}

    private Employee(Builder builder) {
        this.businessId = builder.businessId;
        this.name = builder.name;
        this.phoneCountryCode = builder.phoneCountryCode;
        this.phoneNumber = builder.phoneNumber;
        this.email = builder.email;
        this.bio = builder.bio;
        this.profileImageUrl = builder.profileImageUrl;
        this.status = builder.status;
        this.owner = builder.owner;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateProfile(String name, String email, String bio, String profileImageUrl) {
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
    }

    public void activate() {
        this.status = EmployeeStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = EmployeeStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == EmployeeStatus.ACTIVE;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
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

    public String getBio() {
        return bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public boolean isOwner() {
        return owner;
    }

    public static class Builder {
        private Long businessId;
        private String name;
        private String phoneCountryCode = "+90";
        private String phoneNumber;
        private String email;
        private String bio;
        private String profileImageUrl;
        private EmployeeStatus status = EmployeeStatus.ACTIVE;
        private boolean owner = false;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
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

        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public Builder profileImageUrl(String url) {
            this.profileImageUrl = url;
            return this;
        }

        public Builder status(EmployeeStatus status) {
            this.status = status;
            return this;
        }

        public Builder owner(boolean owner) {
            this.owner = owner;
            return this;
        }

        public Employee build() {
            if (businessId == null) {
                throw new IllegalArgumentException("İşletme ID zorunludur");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Çalışan adı zorunludur");
            }
            return new Employee(this);
        }
    }
}
