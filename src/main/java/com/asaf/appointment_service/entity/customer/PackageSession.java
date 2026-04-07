package com.asaf.appointment_service.entity.customer;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "package_sessions")
public class PackageSession extends BaseEntity {

    @Column(name = "customer_package_id", nullable = false)
    private Long customerPackageId;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageSessionStatus status = PackageSessionStatus.PENDING;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(length = 500)
    private String notes;

    protected PackageSession() {}

    private PackageSession(Builder builder) {
        this.customerPackageId = builder.customerPackageId;
        this.sessionNumber = builder.sessionNumber;
        this.status = builder.status != null ? builder.status : PackageSessionStatus.PENDING;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void assignAppointment(Long appointmentId, LocalDateTime scheduledAt) {
        this.appointmentId = appointmentId;
        this.scheduledAt = scheduledAt;
        this.status = PackageSessionStatus.SCHEDULED;
    }

    public void unassignAppointment() {
        this.appointmentId = null;
        this.scheduledAt = null;
        this.status = PackageSessionStatus.PENDING;
    }

    public void markCompleted() {
        this.status = PackageSessionStatus.COMPLETED;
    }

    public void markNoShow() {
        this.status = PackageSessionStatus.NO_SHOW;
    }

    public void markCancelled() {
        this.status = PackageSessionStatus.CANCELLED;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCustomerPackageId() {
        return customerPackageId;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public PackageSessionStatus getStatus() {
        return status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getNotes() {
        return notes;
    }

    public static class Builder {
        private Long customerPackageId;
        private int sessionNumber;
        private PackageSessionStatus status = PackageSessionStatus.PENDING;

        public Builder customerPackageId(Long customerPackageId) {
            this.customerPackageId = customerPackageId;
            return this;
        }

        public Builder sessionNumber(int sessionNumber) {
            this.sessionNumber = sessionNumber;
            return this;
        }

        public Builder status(PackageSessionStatus status) {
            this.status = status;
            return this;
        }

        public PackageSession build() {
            if (customerPackageId == null) {
                throw new IllegalArgumentException("Müşteri paketi ID zorunludur");
            }
            if (sessionNumber <= 0) {
                throw new IllegalArgumentException("Seans numarası pozitif olmalıdır");
            }
            return new PackageSession(this);
        }
    }
}
