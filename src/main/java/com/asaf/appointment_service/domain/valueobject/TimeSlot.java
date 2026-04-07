package com.asaf.appointment_service.domain.valueobject;

import java.time.Duration;
import java.time.LocalDateTime;

public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Başlangıç ve bitiş zamanı zorunludur");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Başlangıç zamanı bitiş zamanından sonra olamaz");
        }
    }

    public static TimeSlot of(LocalDateTime start, LocalDateTime end) {
        return new TimeSlot(start, end);
    }

    public static TimeSlot of(LocalDateTime start, int durationMinutes) {
        return new TimeSlot(start, start.plusMinutes(durationMinutes));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public long durationMinutes() {
        return duration().toMinutes();
    }

    public boolean overlapsWith(TimeSlot other) {
        return !this.end.isBefore(other.start) && !other.end.isBefore(this.start);
    }

    public boolean contains(LocalDateTime time) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    public boolean isInPast() {
        return end.isBefore(LocalDateTime.now());
    }

    public boolean isInFuture() {
        return start.isAfter(LocalDateTime.now());
    }
}
