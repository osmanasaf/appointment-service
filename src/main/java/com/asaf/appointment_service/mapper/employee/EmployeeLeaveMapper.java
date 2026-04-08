package com.asaf.appointment_service.mapper.employee;

import com.asaf.appointment_service.dto.employee.ConflictingAppointmentDTO;
import com.asaf.appointment_service.dto.employee.LeaveResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.employee.EmployeeLeave;
import java.util.List;

public class EmployeeLeaveMapper {

    private EmployeeLeaveMapper() {}

    public static LeaveResponse toResponse(EmployeeLeave leave) {
        return new LeaveResponse(
            leave.getId(),
            leave.getEmployeeId(),
            leave.getStartDate(),
            leave.getEndDate(),
            leave.getStartTime(),
            leave.getEndTime(),
            leave.isFullDay(),
            leave.getLeaveType(),
            leave.getStatus(),
            leave.getReason(),
            leave.getCreatedAt()
        );
    }

    public static List<LeaveResponse> toResponseList(List<EmployeeLeave> leaves) {
        return leaves.stream().map(EmployeeLeaveMapper::toResponse).toList();
    }

    public static ConflictingAppointmentDTO toConflictingDTO(Appointment appointment) {
        return new ConflictingAppointmentDTO(
            appointment.getId(),
            appointment.getCustomerId(),
            appointment.getServiceId(),
            appointment.getScheduledAt(),
            appointment.getStatus()
        );
    }

    public static List<ConflictingAppointmentDTO> toConflictingDTOList(List<Appointment> appointments) {
        return appointments.stream().map(EmployeeLeaveMapper::toConflictingDTO).toList();
    }
}
