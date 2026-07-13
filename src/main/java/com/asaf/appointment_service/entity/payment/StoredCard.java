package com.asaf.appointment_service.entity.payment;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "stored_cards")
public class StoredCard extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    /**
     * AES-GCM ile şifrelenmiş ctoken (PayTR kart tokenı).
     * Ham değeri asla loglamayın; sadece EncryptionService üzerinden erişin.
     */
    @Column(name = "ctoken_encrypted", nullable = false, length = 512)
    private String ctokenEncrypted;

    @Column(name = "last4", nullable = false, length = 4)
    private String last4;

    @Column(name = "card_brand", length = 32)
    private String cardBrand;

    @Column(name = "expiry_month", nullable = false)
    private int expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private int expiryYear;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    protected StoredCard() {}

    private StoredCard(Builder builder) {
        this.businessId = builder.businessId;
        this.ctokenEncrypted = builder.ctokenEncrypted;
        this.last4 = builder.last4;
        this.cardBrand = builder.cardBrand;
        this.expiryMonth = builder.expiryMonth;
        this.expiryYear = builder.expiryYear;
        this.isDefault = builder.isDefault;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void removeDefault() {
        this.isDefault = false;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getCtokenEncrypted() {
        return ctokenEncrypted;
    }

    public String getLast4() {
        return last4;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public static class Builder {
        private Long businessId;
        private String ctokenEncrypted;
        private String last4;
        private String cardBrand;
        private int expiryMonth;
        private int expiryYear;
        private boolean isDefault;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder ctokenEncrypted(String ctokenEncrypted) {
            this.ctokenEncrypted = ctokenEncrypted;
            return this;
        }

        public Builder last4(String last4) {
            this.last4 = last4;
            return this;
        }

        public Builder cardBrand(String cardBrand) {
            this.cardBrand = cardBrand;
            return this;
        }

        public Builder expiryMonth(int expiryMonth) {
            this.expiryMonth = expiryMonth;
            return this;
        }

        public Builder expiryYear(int expiryYear) {
            this.expiryYear = expiryYear;
            return this;
        }

        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public StoredCard build() {
            if (businessId == null || ctokenEncrypted == null || last4 == null) {
                throw new IllegalArgumentException("StoredCard için zorunlu alanlar eksik");
            }
            if (last4.length() != 4) {
                throw new IllegalArgumentException("last4 tam olarak 4 karakter olmalıdır");
            }
            return new StoredCard(this);
        }
    }
}
