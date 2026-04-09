package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.dto.appointment.AvailableSlotResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.business.BusinessWorkingHoursRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private static final int SLOT_INTERVAL_MINUTES = 15;

    private final BusinessService businessService;
    private final EmployeeService employeeService;
    private final EmployeeCapabilityService capabilityService;
    private final EmployeeScheduleService scheduleService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final AppointmentRepository appointmentRepository;
    private final BusinessWorkingHoursRepository workingHoursRepository;

    public AvailabilityService(BusinessService businessService,
                               EmployeeService employeeService,
                               EmployeeCapabilityService capabilityService,
                               EmployeeScheduleService scheduleService,
                               ServiceDefinitionService serviceDefinitionService,
                               AppointmentRepository appointmentRepository,
                               BusinessWorkingHoursRepository workingHoursRepository) {
        this.businessService = businessService;
        this.employeeService = employeeService;
        this.capabilityService = capabilityService;
        this.scheduleService = scheduleService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.appointmentRepository = appointmentRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long businessId, Long employeeId,
                                                          Long serviceId, LocalDate date) {
        Business business = businessService.findByIdOrThrow(businessId);
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(serviceId);

        validateBookingAllowed(business, date, service);

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        if (employeeId != null) {
            employeeService.findByIdOrThrow(employeeId);
            capabilityService.validateCapability(employeeId, serviceId);
            int duration = capabilityService.effectiveDuration(employeeId, serviceId, service.totalDurationMinutes());
            List<Appointment> existing = appointmentRepository
                .findActiveByEmployeeAndDateRange(employeeId, dayStart, dayEnd);
            return buildSlotsForEmployee(businessId, employeeId, date, duration, business, service, existing);
        }

        return buildSlotsForAnyCapableEmployee(businessId, serviceId, date, business, service);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(Long businessId, Long employeeId, Long serviceId, int daysAhead) {
        Business business = businessService.findByIdOrThrow(businessId);
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(serviceId);

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(Math.min(daysAhead, business.getMaxAdvanceBookingDays()));

        LocalDateTime rangeStart = today.atStartOfDay();
        LocalDateTime rangeEnd = maxDate.plusDays(1).atStartOfDay();

        List<Appointment> existingInRange = employeeId != null
            ? appointmentRepository.findActiveByEmployeeAndDateRange(employeeId, rangeStart, rangeEnd)
            : List.of();

        List<LocalDate> availableDates = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(maxDate); date = date.plusDays(1)) {
            if (isSameDayBlocked(business, service, date, today)) {
                continue;
            }
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            List<Appointment> dayAppointments = filterByDay(existingInRange, dayStart, dayEnd);
            if (hasAvailableSlot(businessId, employeeId, serviceId, date, business, service, dayAppointments)) {
                availableDates.add(date);
            }
        }
        return availableDates;
    }

    private boolean isSameDayBlocked(Business business, ServiceDefinition service,
                                      LocalDate date, LocalDate today) {
        if (!date.equals(today)) {
            return false;
        }
        return !business.isSameDayBookingAllowed() || !service.isSameDayBookingAllowed();
    }

    private List<Appointment> filterByDay(List<Appointment> appointments,
                                           LocalDateTime dayStart, LocalDateTime dayEnd) {
        return appointments.stream()
            .filter(a -> !a.getScheduledAt().isBefore(dayStart) && a.getScheduledAt().isBefore(dayEnd))
            .toList();
    }

    private boolean hasAvailableSlot(Long businessId, Long employeeId, Long serviceId,
                                      LocalDate date, Business business, ServiceDefinition service,
                                      List<Appointment> dayAppointments) {
        if (employeeId != null) {
            int duration = capabilityService.effectiveDuration(employeeId, serviceId, service.totalDurationMinutes());
            return !buildSlotsForEmployee(businessId, employeeId, date, duration, business, service,
                dayAppointments).isEmpty();
        }
        return !buildSlotsForAnyCapableEmployee(businessId, serviceId, date, business, service).isEmpty();
    }

    private List<AvailableSlotResponse> buildSlotsForAnyCapableEmployee(Long businessId, Long serviceId,
                                                                          LocalDate date, Business business,
                                                                          ServiceDefinition service) {
        List<EmployeeServiceCapability> capabilities =
            capabilityService.getActiveByServiceAndBusiness(serviceId, businessId);

        if (capabilities.isEmpty()) {
            return List.of();
        }

        Set<LocalDateTime> seenStarts = new LinkedHashSet<>();
        List<AvailableSlotResponse> merged = new ArrayList<>();

        for (EmployeeServiceCapability cap : capabilities) {
            int duration = cap.effectiveDuration(service.totalDurationMinutes());
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            List<Appointment> existing = appointmentRepository
                .findActiveByEmployeeAndDateRange(cap.getEmployeeId(), dayStart, dayEnd);
            List<AvailableSlotResponse> employeeSlots =
                buildSlotsForEmployee(businessId, cap.getEmployeeId(), date, duration, business, service, existing);
            for (AvailableSlotResponse slot : employeeSlots) {
                if (seenStarts.add(slot.startTime())) {
                    merged.add(slot);
                }
            }
        }

        return merged;
    }

    private List<AvailableSlotResponse> buildSlotsForEmployee(Long businessId, Long employeeId, LocalDate date,
                                                               int totalDuration, Business business,
                                                               ServiceDefinition service,
                                                               List<Appointment> existingAppointments) {
        BusinessWorkingHours workingHours = workingHoursRepository
            .findByBusinessIdAndDayOfWeek(businessId, date.getDayOfWeek())
            .orElse(null);

        if (workingHours == null || workingHours.isClosed()) {
            return List.of();
        }

        LocalTime slotStart = resolveEmployeeStartTime(employeeId, date, workingHours);
        LocalTime slotEnd = resolveEmployeeEndTime(employeeId, date, workingHours);

        if (slotStart == null || slotEnd == null) {
            return List.of();
        }

        List<AvailableSlotResponse> slots = new ArrayList<>();
        LocalTime currentTime = slotStart;
        LocalDateTime now = LocalDateTime.now();

        while (currentTime.plusMinutes(totalDuration).isBefore(slotEnd) ||
               currentTime.plusMinutes(totalDuration).equals(slotEnd)) {

            LocalDateTime start = LocalDateTime.of(date, currentTime);
            LocalDateTime end = start.plusMinutes(totalDuration);

            if (!start.isBefore(now)
                    && !isSlotInBreakTime(currentTime, currentTime.plusMinutes(totalDuration), workingHours)
                    && isSlotFree(start, end, existingAppointments)) {
                boolean instantConfirm = business.isAutoConfirm() && !service.isRequiresConfirmation();
                slots.add(instantConfirm
                    ? AvailableSlotResponse.instant(start, end, totalDuration)
                    : AvailableSlotResponse.requiresConfirmation(start, end, totalDuration));
            }

            currentTime = currentTime.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return slots;
    }

    private boolean isSlotFree(LocalDateTime start, LocalDateTime end, List<Appointment> existing) {
        return existing.stream().noneMatch(a ->
            a.getScheduledAt().isBefore(end) && a.getEndAt().isAfter(start));
    }

    private LocalTime resolveEmployeeStartTime(Long employeeId, LocalDate date,
                                                BusinessWorkingHours businessHours) {
        return scheduleService.getScheduleForDay(employeeId, date.getDayOfWeek())
            .map(s -> s.isClosed() ? null : laterOf(s.getStartTime(), businessHours.getStartTime()))
            .orElse(businessHours.getStartTime());
    }

    private LocalTime resolveEmployeeEndTime(Long employeeId, LocalDate date,
                                              BusinessWorkingHours businessHours) {
        return scheduleService.getScheduleForDay(employeeId, date.getDayOfWeek())
            .map(s -> s.isClosed() ? null : earlierOf(s.getEndTime(), businessHours.getEndTime()))
            .orElse(businessHours.getEndTime());
    }

    private static LocalTime laterOf(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime earlierOf(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    private void validateBookingAllowed(Business business, LocalDate date, ServiceDefinition service) {
        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Geçmiş tarihe randevu alınamaz");
        }
        if (date.equals(today) && !business.isSameDayBookingAllowed()) {
            throw new IllegalArgumentException("Aynı gün randevu alınamaz");
        }
        if (date.equals(today) && !service.isSameDayBookingAllowed()) {
            throw new IllegalArgumentException("Bu hizmet için aynı gün randevu alınamaz");
        }
        LocalDate maxDate = today.plusDays(business.getMaxAdvanceBookingDays());
        if (date.isAfter(maxDate)) {
            throw new IllegalArgumentException(
                "En fazla %d gün sonrasına randevu alınabilir".formatted(business.getMaxAdvanceBookingDays()));
        }
    }

    private boolean isSlotInBreakTime(LocalTime slotStart, LocalTime slotEnd, BusinessWorkingHours workingHours) {
        if (workingHours.getBreakStart() == null || workingHours.getBreakEnd() == null) {
            return false;
        }
        return slotStart.isBefore(workingHours.getBreakEnd()) && slotEnd.isAfter(workingHours.getBreakStart());
    }
}
