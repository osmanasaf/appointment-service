package com.asaf.appointment_service.entity.employee;

import com.asaf.appointment_service.common.entity.BaseEntity;
import com.asaf.appointment_service.domain.valueobject.LeavePeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(
    name = "employee_leaves",
    indexes = @Index(
        name = "idx_el_employee_dates",
        columnList = "employee_id, start_date, end_date"
    )
)
public class EmployeeLeave extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status = LeaveStatus.ACTIVE;

    @Column(name = "reason", length = 500)
    private String reason;

    protected EmployeeLeave() {}

    private EmployeeLeave(Builder builder) {
        this.employeeId = builder.employeeId;
        this.startDate = builder.period.startDate();
        this.endDate = builder.period.endDate();
        this.startTime = builder.period.startTime();
        this.endTime = builder.period.endTime();
        this.leaveType = builder.leaveType;
        this.reason = builder.reason;
        this.status = LeaveStatus.ACTIVE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void cancel() {
        if (this.status == LeaveStatus.CANCELLED) {
            throw new IllegalStateException("İzin zaten iptal edilmiş");
        }
        this.status = LeaveStatus.CANCELLED;
    }

    public boolean isActive() {
        return this.status == LeaveStatus.ACTIVE;
    }

    public boolean isFullDay() {
        return this.startTime == null;
    }

    public boolean overlapsWithDate(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean overlapsWithDateTime(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        if (!overlapsWithDate(date)) {
            return false;
        }
        if (isFullDay()) {
            return true;
        }
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    public LeavePeriod toPeriod() {
        return new LeavePeriod(startDate, endDate, startTime, endTime);
    }

    public Long getEmployeeId() { return employeeId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public LeaveType getLeaveType() { return leaveType; }
    public LeaveStatus getStatus() { return status; }
    public String getReason() { return reason; }

    public static class Builder {
        private Long employeeId;
        private LeavePeriod period;
        private LeaveType leaveType;
        private String reason;

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder period(LeavePeriod period) {
            this.period = period;
            return this;
        }

        public Builder leaveType(LeaveType leaveType) {
            this.leaveType = leaveType;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public EmployeeLeave build() {
            Objects.requireNonNull(employeeId, "Çalışan ID zorunludur");
            Objects.requireNonNull(period, "İzin dönemi zorunludur");
            Objects.requireNonNull(leaveType, "İzin türü zorunludur");
            return new EmployeeLeave(this);
        }
    }
}
