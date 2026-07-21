package com.asaf.appointment_service.entity.calendar;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Çalışanın kişisel Google Takvimi'nden çekilen dolu blok. Müsaitlik motoru bu bloğu
 * mevcut randevularla aynı şekilde "dolu" sayar. Her poll'da ilgili çalışan için
 * baştan üretilir (bkz. GoogleBusyBlockSyncScheduler) — kalıcı bir senkron kaydı değildir.
 */
@Entity
@Table(name = "employee_external_busy")
public class EmployeeExternalBusy extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "external_event_id", nullable = false, length = 255)
    private String externalEventId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    protected EmployeeExternalBusy() {}

    private EmployeeExternalBusy(Builder builder) {
        this.businessId = builder.businessId;
        this.employeeId = builder.employeeId;
        this.externalEventId = builder.externalEventId;
        this.startAt = builder.startAt;
        this.endAt = builder.endAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public static class Builder {
        private Long businessId;
        private Long employeeId;
        private String externalEventId;
        private LocalDateTime startAt;
        private LocalDateTime endAt;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder externalEventId(String externalEventId) {
            this.externalEventId = externalEventId;
            return this;
        }

        public Builder startAt(LocalDateTime startAt) {
            this.startAt = startAt;
            return this;
        }

        public Builder endAt(LocalDateTime endAt) {
            this.endAt = endAt;
            return this;
        }

        public EmployeeExternalBusy build() {
            if (businessId == null || employeeId == null || externalEventId == null
                    || startAt == null || endAt == null) {
                throw new IllegalArgumentException("EmployeeExternalBusy için zorunlu alanlar eksik");
            }
            return new EmployeeExternalBusy(this);
        }
    }
}
