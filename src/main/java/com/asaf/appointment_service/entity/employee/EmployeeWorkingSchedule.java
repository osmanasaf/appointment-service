package com.asaf.appointment_service.entity.employee;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(
    name = "employee_working_schedules",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ews_employee_day",
        columnNames = {"employee_id", "day_of_week"}
    )
)
public class EmployeeWorkingSchedule extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "break_start")
    private LocalTime breakStart;

    @Column(name = "break_end")
    private LocalTime breakEnd;

    @Column(name = "closed", nullable = false)
    private boolean closed = false;

    protected EmployeeWorkingSchedule() {}

    private EmployeeWorkingSchedule(Builder builder) {
        this.employeeId = builder.employeeId;
        this.dayOfWeek = builder.dayOfWeek;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.breakStart = builder.breakStart;
        this.breakEnd = builder.breakEnd;
        this.closed = builder.closed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateHours(LocalTime startTime, LocalTime endTime,
                             LocalTime breakStart, LocalTime breakEnd) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakStart = breakStart;
        this.breakEnd = breakEnd;
        this.closed = false;
    }

    public void markAsClosed() {
        this.closed = true;
    }

    public boolean isTimeWithinSchedule(LocalTime time) {
        if (closed) return false;
        if (time.isBefore(startTime) || time.isAfter(endTime)) return false;
        if (breakStart != null && breakEnd != null) {
            if (!time.isBefore(breakStart) && time.isBefore(breakEnd)) return false;
        }
        return true;
    }

    public Long getEmployeeId() { return employeeId; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public LocalTime getBreakStart() { return breakStart; }
    public LocalTime getBreakEnd() { return breakEnd; }
    public boolean isClosed() { return closed; }

    public static class Builder {
        private Long employeeId;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime = LocalTime.of(9, 0);
        private LocalTime endTime = LocalTime.of(18, 0);
        private LocalTime breakStart;
        private LocalTime breakEnd;
        private boolean closed = false;

        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder dayOfWeek(DayOfWeek day) { this.dayOfWeek = day; return this; }
        public Builder startTime(LocalTime t) { this.startTime = t; return this; }
        public Builder endTime(LocalTime t) { this.endTime = t; return this; }
        public Builder breakStart(LocalTime t) { this.breakStart = t; return this; }
        public Builder breakEnd(LocalTime t) { this.breakEnd = t; return this; }
        public Builder closed(boolean closed) { this.closed = closed; return this; }

        public EmployeeWorkingSchedule build() {
            Objects.requireNonNull(employeeId, "Employee ID zorunludur");
            Objects.requireNonNull(dayOfWeek, "Gün zorunludur");
            return new EmployeeWorkingSchedule(this);
        }
    }
}
