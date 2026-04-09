package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.common.constants.AppointmentErrorMessages;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.exception.appointment.CustomerDayLimitExceededException;
import com.asaf.appointment_service.exception.appointment.SlotNotAvailableException;
import com.asaf.appointment_service.exception.employee.EmployeeOnLeaveException;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.employee.EmployeeLeaveRepository;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentValidationService {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeCapabilityService employeeCapabilityService;
    private final EmployeeScheduleService employeeScheduleService;
    private final EmployeeLeaveRepository employeeLeaveRepository;

    public AppointmentValidationService(AppointmentRepository appointmentRepository,
                                        EmployeeCapabilityService employeeCapabilityService,
                                        EmployeeScheduleService employeeScheduleService,
                                        EmployeeLeaveRepository employeeLeaveRepository) {
        this.appointmentRepository = appointmentRepository;
        this.employeeCapabilityService = employeeCapabilityService;
        this.employeeScheduleService = employeeScheduleService;
        this.employeeLeaveRepository = employeeLeaveRepository;
    }

    public void validateCapability(Long employeeId, Long serviceId) {
        employeeCapabilityService.validateCapability(employeeId, serviceId);
    }

    public int effectiveDuration(Long employeeId, Long serviceId, int defaultDuration) {
        return employeeCapabilityService.effectiveDuration(employeeId, serviceId, defaultDuration);
    }

    @Transactional(readOnly = true)
    public void validateSlotAvailability(Long employeeId, LocalDateTime start, LocalDateTime end) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(employeeId, start, end);
        if (!conflicts.isEmpty()) {
            throw new SlotNotAvailableException(AppointmentErrorMessages.SLOT_NOT_AVAILABLE);
        }
        validateEmployeeNotOnLeave(employeeId, start);
        validateEmployeeSchedule(employeeId, start);
    }

    @Transactional(readOnly = true)
    public void validateDayLimit(Business business, Long customerId, LocalDateTime scheduledAt) {
        Integer dayLimit = business.getMaxAppointmentsPerCustomerPerDay();
        if (dayLimit == null) {
            return;
        }
        LocalDate date = scheduledAt.toLocalDate();
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        long count = appointmentRepository.countActiveByCustomerOnDate(customerId, dayStart, dayEnd);
        if (count >= dayLimit) {
            throw new CustomerDayLimitExceededException(dayLimit, (int) count);
        }
    }

    private void validateEmployeeSchedule(Long employeeId, LocalDateTime scheduledAt) {
        employeeScheduleService.getScheduleForDay(employeeId, scheduledAt.getDayOfWeek()).ifPresent(schedule -> {
            if (schedule.isClosed()) {
                throw new SlotNotAvailableException(AppointmentErrorMessages.EMPLOYEE_CLOSED_DAY);
            }
            LocalTime time = scheduledAt.toLocalTime();
            if (time.isBefore(schedule.getStartTime()) || !time.isBefore(schedule.getEndTime())) {
                throw new SlotNotAvailableException(AppointmentErrorMessages.EMPLOYEE_OUTSIDE_HOURS);
            }
        });
    }

    private void validateEmployeeNotOnLeave(Long employeeId, LocalDateTime scheduledAt) {
        boolean onLeave = employeeLeaveRepository
            .findActiveByEmployeeIdAndDate(employeeId, scheduledAt.toLocalDate())
            .stream()
            .anyMatch(leave -> leave.overlapsWithDateTime(scheduledAt));
        if (onLeave) {
            throw new EmployeeOnLeaveException();
        }
    }
}
