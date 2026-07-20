package com.asaf.appointment_service.entity.token;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Randevu üzerinde tek kullanımlık public işlemler (erteleme, bekleme listesi
 * teklifi, değerlendirme isteği vb.) için opak, süreli token. Modüller arası
 * paylaşılan altyapı; şu an yalnızca RESCHEDULE tipi kullanılıyor.
 */
@Entity
@Table(name = "public_action_tokens")
public class PublicActionToken extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PublicActionTokenType type;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PublicActionTokenStatus status = PublicActionTokenStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected PublicActionToken() {}

    private PublicActionToken(Builder builder) {
        this.businessId = builder.businessId;
        this.appointmentId = builder.appointmentId;
        this.type = builder.type;
        this.token = builder.token;
        this.status = PublicActionTokenStatus.PENDING;
        this.expiresAt = builder.expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markAsUsed() {
        this.status = PublicActionTokenStatus.USED;
        this.usedAt = Instant.now();
    }

    public void cancel() {
        this.status = PublicActionTokenStatus.CANCELLED;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return status == PublicActionTokenStatus.PENDING && !isExpired();
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public PublicActionTokenType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    public PublicActionTokenStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public static class Builder {
        private Long businessId;
        private Long appointmentId;
        private PublicActionTokenType type;
        private String token;
        private Instant expiresAt;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder appointmentId(Long appointmentId) {
            this.appointmentId = appointmentId;
            return this;
        }

        public Builder type(PublicActionTokenType type) {
            this.type = type;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public PublicActionToken build() {
            if (businessId == null || appointmentId == null || type == null
                    || token == null || expiresAt == null) {
                throw new IllegalStateException("All required fields must be set");
            }
            return new PublicActionToken(this);
        }
    }

    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
