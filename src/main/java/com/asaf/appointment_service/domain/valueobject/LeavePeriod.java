package com.asaf.appointment_service.domain.valueobject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public record LeavePeriod(
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime
) {

    public LeavePeriod {
        Objects.requireNonNull(startDate, "İzin başlangıç tarihi zorunludur");
        Objects.requireNonNull(endDate, "İzin bitiş tarihi zorunludur");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("İzin bitiş tarihi başlangıç tarihinden önce olamaz");
        }
        if ((startTime == null) != (endTime == null)) {
            throw new IllegalArgumentException("Kısmi gün izninde başlangıç ve bitiş saati birlikte belirtilmelidir");
        }
        if (startTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("İzin başlangıç saati bitiş saatinden önce olmalıdır");
        }
    }

    public static LeavePeriod fullDay(LocalDate startDate, LocalDate endDate) {
        return new LeavePeriod(startDate, endDate, null, null);
    }

    public static LeavePeriod partialDay(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return new LeavePeriod(date, date, startTime, endTime);
    }

    public boolean isFullDay() {
        return startTime == null;
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

    public boolean overlapsWithTimeSlot(LocalDate date, LocalTime slotStart, LocalTime slotEnd) {
        if (!overlapsWithDate(date)) {
            return false;
        }
        if (isFullDay()) {
            return true;
        }
        return startTime.isBefore(slotEnd) && endTime.isAfter(slotStart);
    }
}
