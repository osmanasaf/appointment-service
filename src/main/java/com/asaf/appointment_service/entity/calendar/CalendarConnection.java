package com.asaf.appointment_service.entity.calendar;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bir çalışanın kişisel Google Takvimi ile kurulan senkron bağlantısı. Refresh token
 * hiçbir zaman düz metin saklanmaz; EncryptionService (StoredCard ile aynı desen) üzerinden
 * şifrelenip okunur. Access token kısa ömürlü olduğu için önbelleğe alınır, süresi dolunca
 * refresh token ile yenilenir.
 */
@Entity
@Table(name = "calendar_connections")
public class CalendarConnection extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Long employeeId;

    @Column(name = "google_calendar_id", nullable = false, length = 255)
    private String googleCalendarId;

    @Column(name = "refresh_token_encrypted", nullable = false, length = 1000)
    private String refreshTokenEncrypted;

    @Column(name = "access_token_encrypted", length = 1000)
    private String accessTokenEncrypted;

    @Column(name = "access_token_expires_at")
    private LocalDateTime accessTokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CalendarConnectionStatus status;

    @Column(name = "broken_reason", length = 500)
    private String brokenReason;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    protected CalendarConnection() {}

    private CalendarConnection(Builder builder) {
        this.businessId = builder.businessId;
        this.employeeId = builder.employeeId;
        this.googleCalendarId = builder.googleCalendarId;
        this.refreshTokenEncrypted = builder.refreshTokenEncrypted;
        this.status = CalendarConnectionStatus.CONNECTED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateTokens(String refreshTokenEncrypted, String accessTokenEncrypted,
                              LocalDateTime accessTokenExpiresAt) {
        if (refreshTokenEncrypted != null) {
            this.refreshTokenEncrypted = refreshTokenEncrypted;
        }
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.status = CalendarConnectionStatus.CONNECTED;
        this.brokenReason = null;
    }

    public void markBroken(String reason) {
        this.status = CalendarConnectionStatus.BROKEN;
        this.brokenReason = reason;
    }

    public void markDisconnected() {
        this.status = CalendarConnectionStatus.DISCONNECTED;
        this.accessTokenEncrypted = null;
        this.accessTokenExpiresAt = null;
    }

    public void updateLastSyncedAt() {
        this.lastSyncedAt = LocalDateTime.now();
    }

    public boolean isAccessTokenValid() {
        return accessTokenEncrypted != null && accessTokenExpiresAt != null
            && accessTokenExpiresAt.isAfter(LocalDateTime.now().plusMinutes(1));
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getGoogleCalendarId() {
        return googleCalendarId;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public LocalDateTime getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public CalendarConnectionStatus getStatus() {
        return status;
    }

    public String getBrokenReason() {
        return brokenReason;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public static class Builder {
        private Long businessId;
        private Long employeeId;
        private String googleCalendarId = "primary";
        private String refreshTokenEncrypted;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder googleCalendarId(String googleCalendarId) {
            this.googleCalendarId = googleCalendarId;
            return this;
        }

        public Builder refreshTokenEncrypted(String refreshTokenEncrypted) {
            this.refreshTokenEncrypted = refreshTokenEncrypted;
            return this;
        }

        public CalendarConnection build() {
            if (businessId == null || employeeId == null) {
                throw new IllegalArgumentException("İşletme ve çalışan ID zorunludur");
            }
            if (refreshTokenEncrypted == null || refreshTokenEncrypted.isBlank()) {
                throw new IllegalArgumentException("Refresh token zorunludur");
            }
            return new CalendarConnection(this);
        }
    }
}
