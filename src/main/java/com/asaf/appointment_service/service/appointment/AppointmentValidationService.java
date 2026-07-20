package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.common.constants.AppointmentErrorMessages;
import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
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
    public void validateSlotAvailability(Long businessId, Long employeeId, LocalDateTime start, LocalDateTime end) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointmentsForBusiness(
            businessId, employeeId, start, end);
        if (!conflicts.isEmpty()) {
            throw new SlotNotAvailableException(AppointmentErrorMessages.SLOT_NOT_AVAILABLE);
        }
        validateEmployeeNotOnLeave(employeeId, start);
        validateEmployeeSchedule(employeeId, start);
    }

    @Transactional(readOnly = true)
    public void validateSlotAvailabilityExcluding(
            Long businessId,
            Long employeeId,
            LocalDateTime start,
            LocalDateTime end,
            Long excludeAppointmentId) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointmentsForBusinessExcluding(
            businessId, employeeId, start, end, excludeAppointmentId);
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

    @Transactional(readOnly = true)
    public void validateDayLimitExcluding(
            Business business,
            Long customerId,
            LocalDateTime scheduledAt,
            Long excludeAppointmentId) {
        Integer dayLimit = business.getMaxAppointmentsPerCustomerPerDay();
        if (dayLimit == null) {
            return;
        }
        LocalDate date = scheduledAt.toLocalDate();
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        long count = appointmentRepository.countActiveByCustomerOnDateExcluding(
            customerId, dayStart, dayEnd, excludeAppointmentId);
        if (count >= dayLimit) {
            throw new CustomerDayLimitExceededException(dayLimit, (int) count);
        }
    }

    /**
     * İşletme/hizmetin rezervasyon politikasını doğrular: geçmiş tarih, aynı gün
     * yasağı (işletme veya hizmet bazlı) ve azami ileri tarih sınırı.
     * Hem online müsaitlik listelemesinde hem müşteri self-servis erteleme akışında
     * tek kaynak olarak kullanılır.
     */
    public void validateBookingPolicy(Business business, LocalDate date, ServiceDefinition service) {
        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            throw new ValidationException("PAST_DATE_BOOKING", "Geçmiş tarihe randevu alınamaz");
        }
        if (date.equals(today) && !business.isSameDayBookingAllowed()) {
            throw new ValidationException("SAME_DAY_BOOKING_NOT_ALLOWED", "Aynı gün randevu alınamaz");
        }
        if (date.equals(today) && !service.isSameDayBookingAllowed()) {
            throw new ValidationException("SERVICE_SAME_DAY_NOT_ALLOWED", "Bu hizmet için aynı gün randevu alınamaz");
        }
        LocalDate maxDate = today.plusDays(business.getMaxAdvanceBookingDays());
        if (date.isAfter(maxDate)) {
            throw new ValidationException("MAX_ADVANCE_BOOKING_EXCEEDED",
                "En fazla %d gün sonrasına randevu alınabilir".formatted(business.getMaxAdvanceBookingDays()));
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
