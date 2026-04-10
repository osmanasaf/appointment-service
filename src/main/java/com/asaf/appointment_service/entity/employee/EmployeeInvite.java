package com.asaf.appointment_service.entity.employee;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "employee_invites")
public class EmployeeInvite extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status = InviteStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected EmployeeInvite() {}

    private EmployeeInvite(Builder builder) {
        this.businessId = builder.businessId;
        this.employeeId = builder.employeeId;
        this.token = builder.token;
        this.email = builder.email;
        this.status = InviteStatus.PENDING;
        this.expiresAt = builder.expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markAsUsed() {
        this.status = InviteStatus.USED;
        this.usedAt = Instant.now();
    }

    public void cancel() {
        this.status = InviteStatus.CANCELLED;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return status == InviteStatus.PENDING && !isExpired();
    }

    public Long getBusinessId() { return businessId; }
    public Long getEmployeeId() { return employeeId; }
    public String getToken() { return token; }
    public String getEmail() { return email; }
    public InviteStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }

    public static class Builder {
        private Long businessId;
        private Long employeeId;
        private String token;
        private String email;
        private Instant expiresAt;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public EmployeeInvite build() {
            if (businessId == null || employeeId == null || token == null || email == null || expiresAt == null) {
                throw new IllegalStateException("All required fields must be set");
            }
            return new EmployeeInvite(this);
        }
    }

    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static Instant defaultExpiry() {
        return Instant.now().plus(7, ChronoUnit.DAYS);
    }
}
