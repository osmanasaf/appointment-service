package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.dto.employee.EmployeeScheduleRequest;
import com.asaf.appointment_service.dto.employee.EmployeeScheduleResponse;
import com.asaf.appointment_service.entity.employee.EmployeeWorkingSchedule;
import com.asaf.appointment_service.repository.employee.EmployeeWorkingScheduleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeScheduleService {

    private final EmployeeWorkingScheduleRepository scheduleRepository;
    private final EmployeeService employeeService;

    public EmployeeScheduleService(EmployeeWorkingScheduleRepository scheduleRepository,
                                    EmployeeService employeeService) {
        this.scheduleRepository = scheduleRepository;
        this.employeeService = employeeService;
    }

    @Transactional
    public EmployeeWorkingSchedule setDaySchedule(Long employeeId, EmployeeScheduleRequest request) {
        employeeService.findByIdOrThrow(employeeId);

        return scheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, request.dayOfWeek())
            .map(existing -> updateExisting(existing, request))
            .orElseGet(() -> createNew(employeeId, request));
    }

    @Transactional(readOnly = true)
    public List<EmployeeWorkingSchedule> getSchedule(Long employeeId) {
        employeeService.findByIdOrThrow(employeeId);
        return scheduleRepository.findByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<EmployeeWorkingSchedule> getScheduleForDay(Long employeeId,
                                                                           java.time.DayOfWeek day) {
        return scheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, day);
    }

    public static EmployeeScheduleResponse toResponse(EmployeeWorkingSchedule entity) {
        return new EmployeeScheduleResponse(
            entity.getId(),
            entity.getEmployeeId(),
            entity.getDayOfWeek(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getBreakStart(),
            entity.getBreakEnd(),
            entity.isClosed()
        );
    }

    private EmployeeWorkingSchedule updateExisting(EmployeeWorkingSchedule existing,
                                                    EmployeeScheduleRequest request) {
        if (request.closed()) {
            existing.markAsClosed();
        } else {
            existing.updateHours(
                request.startTime(),
                request.endTime(),
                request.breakStart(),
                request.breakEnd()
            );
        }
        return scheduleRepository.save(existing);
    }

    private EmployeeWorkingSchedule createNew(Long employeeId, EmployeeScheduleRequest request) {
        EmployeeWorkingSchedule schedule = EmployeeWorkingSchedule.builder()
            .employeeId(employeeId)
            .dayOfWeek(request.dayOfWeek())
            .startTime(request.startTime() != null ? request.startTime() : java.time.LocalTime.of(9, 0))
            .endTime(request.endTime() != null ? request.endTime() : java.time.LocalTime.of(18, 0))
            .breakStart(request.breakStart())
            .breakEnd(request.breakEnd())
            .closed(request.closed())
            .build();
        return scheduleRepository.save(schedule);
    }
}
