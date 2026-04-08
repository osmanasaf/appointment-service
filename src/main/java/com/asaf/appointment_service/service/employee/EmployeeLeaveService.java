package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.domain.valueobject.LeavePeriod;
import com.asaf.appointment_service.dto.employee.ConflictingAppointmentDTO;
import com.asaf.appointment_service.dto.employee.CreateLeaveRequest;
import com.asaf.appointment_service.dto.employee.LeaveConflictAction;
import com.asaf.appointment_service.dto.employee.LeaveConflictPreviewResponse;
import com.asaf.appointment_service.dto.employee.LeaveResponse;
import com.asaf.appointment_service.dto.employee.ResolveLeaveConflictRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.employee.EmployeeLeave;
import com.asaf.appointment_service.exception.employee.EmployeeLeaveNotFoundException;
import com.asaf.appointment_service.mapper.employee.EmployeeLeaveMapper;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.employee.EmployeeLeaveRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeLeaveService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeLeaveService.class);
    private static final String LEAVE_CANCEL_REASON = "Çalışan izni nedeniyle randevu iptal edildi";

    private final EmployeeLeaveRepository leaveRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final EmployeeService employeeService;
    private final EmployeeCapabilityService capabilityService;

    public EmployeeLeaveService(EmployeeLeaveRepository leaveRepository,
                                 AppointmentRepository appointmentRepository,
                                 AppointmentService appointmentService,
                                 EmployeeService employeeService,
                                 EmployeeCapabilityService capabilityService) {
        this.leaveRepository = leaveRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
        this.employeeService = employeeService;
        this.capabilityService = capabilityService;
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> listLeaves(Long employeeId) {
        employeeService.findByIdOrThrow(employeeId);
        return EmployeeLeaveMapper.toResponseList(
            leaveRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
        );
    }

    @Transactional(readOnly = true)
    public LeaveConflictPreviewResponse previewConflicts(Long employeeId, CreateLeaveRequest request) {
        employeeService.findByIdOrThrow(employeeId);
        List<ConflictingAppointmentDTO> conflicts = findConflictingAppointments(employeeId, request);
        return LeaveConflictPreviewResponse.of(conflicts);
    }

    @Transactional
    public LeaveResponse createLeave(Long employeeId, CreateLeaveRequest request) {
        employeeService.findByIdOrThrow(employeeId);
        validateLeaveRequest(request);
        LeavePeriod period = buildPeriod(request);
        EmployeeLeave leave = EmployeeLeave.builder()
            .employeeId(employeeId)
            .period(period)
            .leaveType(request.leaveType())
            .reason(request.reason())
            .build();
        EmployeeLeave saved = leaveRepository.save(leave);
        log.info("İzin oluşturuldu: employeeId={}, startDate={}, endDate={}", employeeId, request.startDate(), request.endDate());
        return EmployeeLeaveMapper.toResponse(saved);
    }

    @Transactional
    public LeaveResponse resolveConflictsAndCreateLeave(Long employeeId,
                                                         CreateLeaveRequest leaveRequest,
                                                         ResolveLeaveConflictRequest resolveRequest) {
        employeeService.findByIdOrThrow(employeeId);
        validateLeaveRequest(leaveRequest);
        validateResolveRequest(resolveRequest);

        List<Appointment> conflicting = findRawConflictingAppointments(employeeId, leaveRequest);
        resolveConflicts(conflicting, resolveRequest);

        LeavePeriod period = buildPeriod(leaveRequest);
        EmployeeLeave leave = EmployeeLeave.builder()
            .employeeId(employeeId)
            .period(period)
            .leaveType(leaveRequest.leaveType())
            .reason(leaveRequest.reason())
            .build();
        EmployeeLeave saved = leaveRepository.save(leave);
        log.info("İzin çakışmaları çözüldü ve izin oluşturuldu: employeeId={}, action={}, conflicts={}",
            employeeId, resolveRequest.action(), conflicting.size());
        return EmployeeLeaveMapper.toResponse(saved);
    }

    @Transactional
    public void cancelLeave(Long employeeId, Long leaveId) {
        employeeService.findByIdOrThrow(employeeId);
        EmployeeLeave leave = leaveRepository.findById(leaveId)
            .filter(l -> l.getEmployeeId().equals(employeeId))
            .orElseThrow(() -> new EmployeeLeaveNotFoundException(leaveId));
        if (leave.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Geçmiş izinler iptal edilemez");
        }
        leave.cancel();
        leaveRepository.save(leave);
        log.info("İzin iptal edildi: leaveId={}, employeeId={}", leaveId, employeeId);
    }

    private void validateLeaveRequest(CreateLeaveRequest request) {
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("İzin başlangıç tarihi geçmişte olamaz");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("İzin bitiş tarihi başlangıç tarihinden önce olamaz");
        }
        boolean hasStartTime = request.startTime() != null;
        boolean hasEndTime = request.endTime() != null;
        if (hasStartTime != hasEndTime) {
            throw new IllegalArgumentException("Kısmi gün izninde başlangıç ve bitiş saati birlikte belirtilmelidir");
        }
        if (hasStartTime && !request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("İzin başlangıç saati bitiş saatinden önce olmalıdır");
        }
    }

    private void validateResolveRequest(ResolveLeaveConflictRequest request) {
        if (request.action() == LeaveConflictAction.REASSIGN_ALL) {
            Objects.requireNonNull(request.newEmployeeId(),
                "Yeniden atama için yeni çalışan ID zorunludur");
            employeeService.findByIdOrThrow(request.newEmployeeId());
        }
    }

    private LeavePeriod buildPeriod(CreateLeaveRequest request) {
        return new LeavePeriod(
            request.startDate(),
            request.endDate(),
            request.startTime(),
            request.endTime()
        );
    }

    private List<ConflictingAppointmentDTO> findConflictingAppointments(Long employeeId, CreateLeaveRequest request) {
        return EmployeeLeaveMapper.toConflictingDTOList(findRawConflictingAppointments(employeeId, request));
    }

    private List<Appointment> findRawConflictingAppointments(Long employeeId, CreateLeaveRequest request) {
        LocalDateTime startDateTime = request.startDate().atStartOfDay();
        LocalDateTime endDateTime = request.endDate().atTime(23, 59, 59);
        return appointmentRepository.findConflictingAppointments(employeeId, startDateTime, endDateTime);
    }

    private void resolveConflicts(List<Appointment> appointments, ResolveLeaveConflictRequest request) {
        if (request.action() == LeaveConflictAction.CANCEL_ALL) {
            appointments.forEach(a -> appointmentService.cancelAppointment(a.getId(), LEAVE_CANCEL_REASON));
        } else {
            appointments.forEach(a -> {
                capabilityService.validateCapability(request.newEmployeeId(), a.getServiceId());
                appointmentService.reassignAppointmentEmployee(a.getId(), request.newEmployeeId());
            });
        }
    }
}
