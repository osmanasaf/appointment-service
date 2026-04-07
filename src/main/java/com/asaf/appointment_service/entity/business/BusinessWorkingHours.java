package com.asaf.appointment_service.entity.business;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "business_working_hours")
public class BusinessWorkingHours extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

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

    @Column(name = "is_closed")
    private boolean closed = false;

    protected BusinessWorkingHours() {}

    public BusinessWorkingHours(Long businessId, DayOfWeek dayOfWeek, 
                                 LocalTime startTime, LocalTime endTime) {
        this.businessId = businessId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static BusinessWorkingHours defaultHours(Long businessId, DayOfWeek day) {
        BusinessWorkingHours hours = new BusinessWorkingHours();
        hours.businessId = businessId;
        hours.dayOfWeek = day;
        hours.startTime = LocalTime.of(9, 0);
        hours.endTime = LocalTime.of(18, 0);
        hours.breakStart = LocalTime.of(12, 0);
        hours.breakEnd = LocalTime.of(13, 0);
        hours.closed = (day == DayOfWeek.SUNDAY);
        return hours;
    }

    public static BusinessWorkingHours closedDay(Long businessId, DayOfWeek day) {
        BusinessWorkingHours hours = new BusinessWorkingHours();
        hours.businessId = businessId;
        hours.dayOfWeek = day;
        hours.startTime = LocalTime.of(0, 0);
        hours.endTime = LocalTime.of(0, 0);
        hours.closed = true;
        return hours;
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

    public void markAsOpen() {
        this.closed = false;
    }

    public boolean isTimeWithinWorkingHours(LocalTime time) {
        if (closed) return false;
        if (time.isBefore(startTime) || time.isAfter(endTime)) return false;
        if (breakStart != null && breakEnd != null) {
            if (!time.isBefore(breakStart) && time.isBefore(breakEnd)) return false;
        }
        return true;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalTime getBreakStart() {
        return breakStart;
    }

    public LocalTime getBreakEnd() {
        return breakEnd;
    }

    public boolean isClosed() {
        return closed;
    }
}
