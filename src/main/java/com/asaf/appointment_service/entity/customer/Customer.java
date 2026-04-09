package com.asaf.appointment_service.entity.customer;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_country_code")
    private String phoneCountryCode;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column
    private String email;

    @Column(length = 500)
    private String notes;

    @Column(name = "no_show_count")
    private int noShowCount = 0;

    @Column(name = "is_blacklisted")
    private boolean blacklisted = false;

    @Column(name = "blacklist_reason")
    private String blacklistReason;

    @Column(length = 500)
    private String address;

    @Column(name = "tc_encrypted", length = 500)
    private String tcEncrypted;

    @Column(name = "no_show_forces_manual_approval", nullable = false)
    private boolean noShowForcesManualApproval = false;

    protected Customer() {}

    private Customer(Builder builder) {
        if (builder.id != null) this.setId(builder.id);
        this.businessId = builder.businessId;
        this.name = builder.name;
        this.phoneCountryCode = builder.phoneCountryCode;
        this.phoneNumber = builder.phoneNumber;
        this.email = builder.email;
        this.notes = builder.notes;
        this.address = builder.address;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateProfile(String name, String email, String notes, String address, String tcEncrypted) {
        this.name = name;
        this.email = email;
        this.notes = notes;
        this.address = address;
        this.tcEncrypted = tcEncrypted;
    }

    public void incrementNoShow() {
        this.noShowCount++;
    }

    public void blacklist(String reason) {
        this.blacklisted = true;
        this.blacklistReason = reason;
    }

    public void removeFromBlacklist() {
        this.blacklisted = false;
        this.blacklistReason = null;
    }

    public void setNoShowForcesManualApproval(boolean value) {
        this.noShowForcesManualApproval = value;
    }

    public boolean isNoShowForcesManualApproval() {
        return noShowForcesManualApproval;
    }

    public boolean isRisky() {
        return noShowCount >= 2;
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

    public String getNotes() {
        return notes;
    }

    public int getNoShowCount() {
        return noShowCount;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public String getBlacklistReason() {
        return blacklistReason;
    }

    public String getAddress() {
        return address;
    }

    public String getTcEncrypted() {
        return tcEncrypted;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTcEncrypted(String tcEncrypted) {
        this.tcEncrypted = tcEncrypted;
    }

    public static class Builder {
        private Long id;
        private Long businessId;
        private String name;
        private String phoneCountryCode = "+90";
        private String phoneNumber;
        private String email;
        private String notes;
        private String address;

        public Builder id(Long id) {
            this.id = id;
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

        public Builder phone(String countryCode, String number) {
            this.phoneCountryCode = countryCode;
            this.phoneNumber = number;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Customer build() {
            if (id == null) {
                if (businessId == null) {
                    throw new IllegalArgumentException("İşletme ID zorunludur");
                }
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("Müşteri adı zorunludur");
                }
                if (phoneNumber == null || phoneNumber.isBlank()) {
                    throw new IllegalArgumentException("Telefon numarası zorunludur");
                }
            }
            return new Customer(this);
        }
    }
}
