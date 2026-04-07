package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.dto.appointment.AvailableSlotResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.business.BusinessWorkingHoursRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private static final int SLOT_INTERVAL_MINUTES = 15;

    private final BusinessService businessService;
    private final EmployeeService employeeService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final AppointmentRepository appointmentRepository;
    private final BusinessWorkingHoursRepository workingHoursRepository;

    public AvailabilityService(BusinessService businessService,
                               EmployeeService employeeService,
                               ServiceDefinitionService serviceDefinitionService,
                               AppointmentRepository appointmentRepository,
                               BusinessWorkingHoursRepository workingHoursRepository) {
        this.businessService = businessService;
        this.employeeService = employeeService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.appointmentRepository = appointmentRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long businessId, Long employeeId, 
                                                          Long serviceId, LocalDate date) {
        Business business = businessService.findByIdOrThrow(businessId);
        employeeService.findByIdOrThrow(employeeId);
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(serviceId);

        validateBookingAllowed(business, date, service);

        BusinessWorkingHours workingHours = workingHoursRepository
            .findByBusinessIdAndDayOfWeek(businessId, date.getDayOfWeek())
            .orElse(null);

        if (workingHours == null || workingHours.isClosed()) {
            return List.of();
        }

        int totalDuration = service.totalDurationMinutes();
        List<AvailableSlotResponse> slots = new ArrayList<>();

        LocalTime currentTime = workingHours.getStartTime();
        LocalTime endTime = workingHours.getEndTime();

        while (currentTime.plusMinutes(totalDuration).isBefore(endTime) || 
               currentTime.plusMinutes(totalDuration).equals(endTime)) {
            
            LocalDateTime slotStart = LocalDateTime.of(date, currentTime);
            LocalDateTime slotEnd = slotStart.plusMinutes(totalDuration);

            if (isSlotInBreakTime(currentTime, currentTime.plusMinutes(totalDuration), workingHours)) {
                currentTime = currentTime.plusMinutes(SLOT_INTERVAL_MINUTES);
                continue;
            }

            if (isSlotAvailable(employeeId, slotStart, slotEnd)) {
                boolean instantConfirm = business.isAutoConfirm() && !service.isRequiresConfirmation();
                
                if (instantConfirm) {
                    slots.add(AvailableSlotResponse.instant(slotStart, slotEnd, totalDuration));
                } else {
                    slots.add(AvailableSlotResponse.requiresConfirmation(slotStart, slotEnd, totalDuration));
                }
            }

            currentTime = currentTime.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return slots;
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(Long businessId, Long employeeId, Long serviceId, int daysAhead) {
        Business business = businessService.findByIdOrThrow(businessId);
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(serviceId);

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(Math.min(daysAhead, business.getMaxAdvanceBookingDays()));

        for (LocalDate date = today; !date.isAfter(maxDate); date = date.plusDays(1)) {
            if (date.equals(today) && !business.isSameDayBookingAllowed()) {
                continue;
            }
            if (!service.isSameDayBookingAllowed() && date.equals(today)) {
                continue;
            }

            List<AvailableSlotResponse> slots = getAvailableSlots(businessId, employeeId, serviceId, date);
            if (!slots.isEmpty()) {
                availableDates.add(date);
            }
        }

        return availableDates;
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
            throw new IllegalArgumentException("En fazla " + business.getMaxAdvanceBookingDays() + " gün sonrasına randevu alınabilir");
        }
    }

    private boolean isSlotInBreakTime(LocalTime slotStart, LocalTime slotEnd, BusinessWorkingHours workingHours) {
        if (workingHours.getBreakStart() == null || workingHours.getBreakEnd() == null) {
            return false;
        }

        LocalTime breakStart = workingHours.getBreakStart();
        LocalTime breakEnd = workingHours.getBreakEnd();

        return (slotStart.isBefore(breakEnd) && slotEnd.isAfter(breakStart));
    }

    private boolean isSlotAvailable(Long employeeId, LocalDateTime start, LocalDateTime end) {
        if (start.isBefore(LocalDateTime.now())) {
            return false;
        }

        return appointmentRepository.findConflictingAppointments(employeeId, start, end).isEmpty();
    }
}
