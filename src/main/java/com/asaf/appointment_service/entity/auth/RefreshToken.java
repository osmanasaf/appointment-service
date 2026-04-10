package com.asaf.appointment_service.entity.auth;

import com.asaf.appointment_service.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_user_revoked", columnList = "user_id, revoked"),
    @Index(name = "idx_refresh_token_expires_revoked", columnList = "expires_at, revoked")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked")
    private boolean revoked = false;

    @Column(name = "device_info")
    private String deviceInfo;

    protected RefreshToken() {}

    private RefreshToken(User user, Instant expiresAt, String deviceInfo) {
        this.token = UUID.randomUUID().toString().replace("-", "");
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.deviceInfo = deviceInfo;
    }

    public static RefreshToken create(User user, long validDays, String deviceInfo) {
        Instant expiry = Instant.now().plusSeconds(validDays * 24 * 60 * 60);
        return new RefreshToken(user, expiry, deviceInfo);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRevoked() { return revoked; }
    public String getDeviceInfo() { return deviceInfo; }
}
