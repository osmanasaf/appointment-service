package com.asaf.appointment_service.entity.appointment;

import com.asaf.appointment_service.common.entity.BaseEntity;
import com.asaf.appointment_service.common.statemachine.StateMachine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

    @Transient
    private static final AppointmentStateMachine STATE_MACHINE = new AppointmentStateMachine();

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "is_risky")
    private boolean risky = false;

    @Column(name = "risk_reason")
    private String riskReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(length = 500)
    private String notes;

    @Column(name = "customer_notes", length = 500)
    private String customerNotes;

    @Column(name = "reminder_sent")
    private boolean reminderSent = false;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private AppointmentSource source = AppointmentSource.ONLINE;

    @Column(name = "package_session_id")
    private Long packageSessionId;

    protected Appointment() {}

    private Appointment(Builder builder) {
        this.businessId = builder.businessId;
        this.customerId = builder.customerId;
        this.employeeId = builder.employeeId;
        this.serviceId = builder.serviceId;
        this.scheduledAt = builder.scheduledAt;
        this.endAt = builder.endAt;
        this.durationMinutes = builder.durationMinutes;
        this.status = builder.status;
        this.notes = builder.notes;
        this.customerNotes = builder.customerNotes;
        this.source = builder.source != null ? builder.source : AppointmentSource.ONLINE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void confirm() {
        STATE_MACHINE.validateTransition(this.status, AppointmentStatus.CONFIRMED);
        this.status = AppointmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.risky = false;
        this.riskReason = null;
    }

    public void cancel(String reason) {
        STATE_MACHINE.validateTransition(this.status, AppointmentStatus.CANCELLED);
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    public void reassignEmployee(Long newEmployeeId) {
        Objects.requireNonNull(newEmployeeId, "Yeni çalışan ID zorunludur");
        this.employeeId = newEmployeeId;
    }

    public void complete() {
        STATE_MACHINE.validateTransition(this.status, AppointmentStatus.COMPLETED);
        this.status = AppointmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsNoShow() {
        STATE_MACHINE.validateTransition(this.status, AppointmentStatus.NO_SHOW);
        this.status = AppointmentStatus.NO_SHOW;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsRisky(String reason) {
        STATE_MACHINE.validateTransition(this.status, AppointmentStatus.RISKY);
        this.status = AppointmentStatus.RISKY;
        this.risky = true;
        this.riskReason = reason;
    }

    public void markReminderSent() {
        this.reminderSent = true;
        this.reminderSentAt = LocalDateTime.now();
    }

    public boolean isUpcoming() {
        return this.scheduledAt.isAfter(LocalDateTime.now());
    }

    public boolean isPast() {
        return this.endAt.isBefore(LocalDateTime.now());
    }

    public boolean isSameDay() {
        return this.scheduledAt.toLocalDate().equals(java.time.LocalDate.now());
    }

    public boolean needsReminder(int hoursBeforeAppointment) {
        if (reminderSent) return false;
        LocalDateTime reminderTime = scheduledAt.minusHours(hoursBeforeAppointment);
        return LocalDateTime.now().isAfter(reminderTime) && isUpcoming();
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public boolean isRisky() {
        return risky;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public AppointmentSource getSource() {
        return source != null ? source : AppointmentSource.ONLINE;
    }

    public Long getPackageSessionId() {
        return packageSessionId;
    }

    public void setPackageSessionId(Long packageSessionId) {
        this.packageSessionId = packageSessionId;
    }

    public static class Builder {
        private Long businessId;
        private Long customerId;
        private Long employeeId;
        private Long serviceId;
        private LocalDateTime scheduledAt;
        private LocalDateTime endAt;
        private int durationMinutes;
        private AppointmentStatus status = AppointmentStatus.PENDING;
        private String notes;
        private String customerNotes;
        private AppointmentSource source = AppointmentSource.ONLINE;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder serviceId(Long serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder scheduledAt(LocalDateTime scheduledAt) {
            this.scheduledAt = scheduledAt;
            return this;
        }

        public Builder endAt(LocalDateTime endAt) {
            this.endAt = endAt;
            return this;
        }

        public Builder durationMinutes(int minutes) {
            this.durationMinutes = minutes;
            return this;
        }

        public Builder timeSlot(LocalDateTime start, int durationMinutes) {
            this.scheduledAt = start;
            this.endAt = start.plusMinutes(durationMinutes);
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder status(AppointmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder customerNotes(String customerNotes) {
            this.customerNotes = customerNotes;
            return this;
        }

        public Builder source(AppointmentSource source) {
            this.source = source;
            return this;
        }

        public Appointment build() {
            if (businessId == null || customerId == null || employeeId == null || serviceId == null) {
                throw new IllegalArgumentException("Business, Customer, Employee ve Service ID zorunludur");
            }
            if (scheduledAt == null || endAt == null) {
                throw new IllegalArgumentException("Randevu zamanı zorunludur");
            }
            return new Appointment(this);
        }
    }

    private static class AppointmentStateMachine extends StateMachine<AppointmentStatus> {
        
        public AppointmentStateMachine() {
            super(AppointmentStatus.class);
        }

        @Override
        protected void defineTransitions() {
            addTransition(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
            addTransition(AppointmentStatus.PENDING, AppointmentStatus.CANCELLED);
            addTransition(AppointmentStatus.PENDING, AppointmentStatus.RISKY);
            addTransition(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED);
            addTransition(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED);
            addTransition(AppointmentStatus.CONFIRMED, AppointmentStatus.NO_SHOW);
            addTransition(AppointmentStatus.RISKY, AppointmentStatus.CONFIRMED);
            addTransition(AppointmentStatus.RISKY, AppointmentStatus.CANCELLED);
        }
    }
}
