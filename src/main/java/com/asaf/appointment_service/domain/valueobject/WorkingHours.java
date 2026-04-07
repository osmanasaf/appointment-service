package com.asaf.appointment_service.domain.valueobject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public record WorkingHours(LocalTime startTime, LocalTime endTime, LocalTime breakStart, LocalTime breakEnd) {

    public WorkingHours {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Başlangıç ve bitiş saati zorunludur");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Başlangıç saati bitiş saatinden önce olmalıdır");
        }
        if (breakStart != null && breakEnd != null) {
            if (!breakStart.isBefore(breakEnd)) {
                throw new IllegalArgumentException("Mola başlangıcı mola bitişinden önce olmalıdır");
            }
            if (breakStart.isBefore(startTime) || breakEnd.isAfter(endTime)) {
                throw new IllegalArgumentException("Mola saatleri çalışma saatleri içinde olmalıdır");
            }
        }
    }

    public static WorkingHours of(LocalTime start, LocalTime end) {
        return new WorkingHours(start, end, null, null);
    }

    public static WorkingHours of(LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) {
        return new WorkingHours(start, end, breakStart, breakEnd);
    }

    public static WorkingHours defaultHours() {
        return new WorkingHours(
            LocalTime.of(9, 0),
            LocalTime.of(18, 0),
            LocalTime.of(12, 0),
            LocalTime.of(13, 0)
        );
    }

    public boolean isWithinWorkingHours(LocalTime time) {
        if (time.isBefore(startTime) || time.isAfter(endTime)) {
            return false;
        }
        if (hasBreak() && !time.isBefore(breakStart) && time.isBefore(breakEnd)) {
            return false;
        }
        return true;
    }

    public boolean hasBreak() {
        return breakStart != null && breakEnd != null;
    }

    public int totalWorkingMinutes() {
        int total = (int) java.time.Duration.between(startTime, endTime).toMinutes();
        if (hasBreak()) {
            total -= (int) java.time.Duration.between(breakStart, breakEnd).toMinutes();
        }
        return total;
    }

    public List<TimeSlot> getAvailableSlots(LocalDate date, int slotDurationMinutes) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = startTime;
        
        while (current.plusMinutes(slotDurationMinutes).isBefore(endTime) 
               || current.plusMinutes(slotDurationMinutes).equals(endTime)) {
            LocalTime slotEnd = current.plusMinutes(slotDurationMinutes);
            
            boolean overlapsBreak = hasBreak() && 
                !(slotEnd.isBefore(breakStart) || slotEnd.equals(breakStart) || current.isAfter(breakEnd) || current.equals(breakEnd));
            
            if (!overlapsBreak) {
                slots.add(TimeSlot.of(
                    LocalDateTime.of(date, current),
                    LocalDateTime.of(date, slotEnd)
                ));
            } else if (current.isBefore(breakStart)) {
                current = breakEnd;
                continue;
            }
            
            current = current.plusMinutes(slotDurationMinutes);
        }
        
        return slots;
    }

    public String displayFormat() {
        String base = String.format("%s - %s", startTime, endTime);
        if (hasBreak()) {
            base += String.format(" (Mola: %s - %s)", breakStart, breakEnd);
        }
        return base;
    }
}
