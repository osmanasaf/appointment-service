package com.asaf.appointment_service.entity.waitlist;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dolu bir saate müşterinin bekleme listesine yazılması. Bir randevu iptal/erteleme
 * ile boşaldığında (bkz. SlotFreedEvent) uygun ilk WAITING kayıt otomatik teklif alır.
 */
@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "preferred_from")
    private LocalDate preferredFrom;

    @Column(name = "preferred_until")
    private LocalDate preferredUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "offered_employee_id")
    private Long offeredEmployeeId;

    @Column(name = "offered_scheduled_at")
    private LocalDateTime offeredScheduledAt;

    @Column(name = "offered_end_at")
    private LocalDateTime offeredEndAt;

    protected WaitlistEntry() {}

    private WaitlistEntry(Builder builder) {
        this.businessId = builder.businessId;
        this.customerId = builder.customerId;
        this.serviceId = builder.serviceId;
        this.employeeId = builder.employeeId;
        this.preferredFrom = builder.preferredFrom;
        this.preferredUntil = builder.preferredUntil;
        this.status = WaitlistStatus.WAITING;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Bu kayıt WAITING durumundaysa ve boşalan slot; hizmeti, (varsa) tercih edilen
     * çalışanı ve tercih penceresini karşılıyorsa true döner.
     */
    public boolean matchesFreedSlot(Long freedServiceId, Long freedEmployeeId, LocalDateTime freedScheduledAt) {
        if (status != WaitlistStatus.WAITING) {
            return false;
        }
        if (!serviceId.equals(freedServiceId)) {
            return false;
        }
        if (employeeId != null && !employeeId.equals(freedEmployeeId)) {
            return false;
        }
        LocalDate freedDate = freedScheduledAt.toLocalDate();
        if (preferredFrom != null && freedDate.isBefore(preferredFrom)) {
            return false;
        }
        return preferredUntil == null || !freedDate.isAfter(preferredUntil);
    }

    public void markNotified(Long offeredEmployeeId, LocalDateTime offeredScheduledAt, LocalDateTime offeredEndAt) {
        if (status != WaitlistStatus.WAITING) {
            throw new IllegalStateException("Sadece bekleyen kayıtlara teklif gönderilebilir");
        }
        this.status = WaitlistStatus.NOTIFIED;
        this.notifiedAt = LocalDateTime.now();
        this.offeredEmployeeId = offeredEmployeeId;
        this.offeredScheduledAt = offeredScheduledAt;
        this.offeredEndAt = offeredEndAt;
    }

    public void markExpired() {
        if (status != WaitlistStatus.NOTIFIED) {
            throw new IllegalStateException("Sadece teklif gönderilmiş kayıtların süresi dolabilir");
        }
        this.status = WaitlistStatus.EXPIRED;
    }

    public void markConverted() {
        if (status != WaitlistStatus.NOTIFIED) {
            throw new IllegalStateException("Sadece teklif gönderilmiş kayıtlar randevuya dönüştürülebilir");
        }
        this.status = WaitlistStatus.CONVERTED;
    }

    public void cancel() {
        if (status != WaitlistStatus.WAITING && status != WaitlistStatus.NOTIFIED) {
            throw new IllegalStateException("Bu kayıt artık iptal edilemez");
        }
        this.status = WaitlistStatus.CANCELLED;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getPreferredFrom() {
        return preferredFrom;
    }

    public LocalDate getPreferredUntil() {
        return preferredUntil;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public Long getOfferedEmployeeId() {
        return offeredEmployeeId;
    }

    public LocalDateTime getOfferedScheduledAt() {
        return offeredScheduledAt;
    }

    public LocalDateTime getOfferedEndAt() {
        return offeredEndAt;
    }

    public static class Builder {
        private Long businessId;
        private Long customerId;
        private Long serviceId;
        private Long employeeId;
        private LocalDate preferredFrom;
        private LocalDate preferredUntil;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder serviceId(Long serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder preferredFrom(LocalDate preferredFrom) {
            this.preferredFrom = preferredFrom;
            return this;
        }

        public Builder preferredUntil(LocalDate preferredUntil) {
            this.preferredUntil = preferredUntil;
            return this;
        }

        public WaitlistEntry build() {
            if (businessId == null || customerId == null || serviceId == null) {
                throw new IllegalArgumentException("İşletme, müşteri ve hizmet ID zorunludur");
            }
            return new WaitlistEntry(this);
        }
    }
}
