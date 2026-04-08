package com.asaf.appointment_service.dto.employee;

import java.util.List;

public record LeaveConflictPreviewResponse(
    List<ConflictingAppointmentDTO> conflictingAppointments,
    int totalCount
) {
    public static LeaveConflictPreviewResponse of(List<ConflictingAppointmentDTO> appointments) {
        return new LeaveConflictPreviewResponse(appointments, appointments.size());
    }

    public boolean hasConflicts() {
        return totalCount > 0;
    }
}
