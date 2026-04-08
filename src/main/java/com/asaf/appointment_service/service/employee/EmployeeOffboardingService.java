package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.dto.employee.AffectedAppointmentResponse;
import com.asaf.appointment_service.dto.employee.OffboardAction;
import com.asaf.appointment_service.dto.employee.OffboardRequest;
import com.asaf.appointment_service.dto.employee.OffboardSummaryResponse;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeOffboardingService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeOffboardingService.class);
    private static final String OFFBOARD_CANCEL_REASON = "Çalışan ayrılması nedeniyle randevu iptal edildi";

    private final EmployeeService employeeService;
    private final EmployeeCapabilityService capabilityService;
    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;

    public EmployeeOffboardingService(EmployeeService employeeService,
                                       EmployeeCapabilityService capabilityService,
                                       AppointmentService appointmentService,
                                       AppointmentRepository appointmentRepository) {
        this.employeeService = employeeService;
        this.capabilityService = capabilityService;
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AffectedAppointmentResponse> previewOffboarding(Long employeeId) {
        employeeService.findByIdOrThrow(employeeId);
        return findFutureAppointments(employeeId).stream()
            .map(a -> new AffectedAppointmentResponse(
                a.getId(), a.getCustomerId(), a.getServiceId(),
                a.getScheduledAt(), a.getStatus()))
            .toList();
    }

    @Transactional
    public OffboardSummaryResponse offboard(Long employeeId, OffboardRequest request) {
        employeeService.findByIdOrThrow(employeeId);
        validateRequest(request);

        List<Appointment> futureAppointments = findFutureAppointments(employeeId);
        int affected = futureAppointments.size();

        if (request.action() == OffboardAction.CANCEL_ALL) {
            cancelAll(futureAppointments);
        } else {
            reassignAll(futureAppointments, request.newEmployeeId());
        }

        deactivateEmployee(employeeId);
        log.info("Çalışan offboard edildi: employeeId={}, action={}, affectedAppointments={}",
            employeeId, request.action(), affected);

        return new OffboardSummaryResponse(
            affected,
            request.action(),
            request.action() == OffboardAction.REASSIGN_ALL ? request.newEmployeeId() : null
        );
    }

    private List<Appointment> findFutureAppointments(Long employeeId) {
        return appointmentRepository.findFutureActiveByEmployeeId(employeeId, LocalDateTime.now());
    }

    private void validateRequest(OffboardRequest request) {
        if (request.action() == OffboardAction.REASSIGN_ALL) {
            Objects.requireNonNull(request.newEmployeeId(),
                "Yeniden atama işlemi için yeni çalışan ID zorunludur");
            employeeService.findByIdOrThrow(request.newEmployeeId());
        }
    }

    private void cancelAll(List<Appointment> appointments) {
        appointments.forEach(a ->
            appointmentService.cancelAppointment(a.getId(), OFFBOARD_CANCEL_REASON));
    }

    private void reassignAll(List<Appointment> appointments, Long newEmployeeId) {
        appointments.forEach(a -> {
            capabilityService.validateCapability(newEmployeeId, a.getServiceId());
            appointmentService.reassignAppointmentEmployee(a.getId(), newEmployeeId);
        });
    }

    private void deactivateEmployee(Long employeeId) {
        employeeService.deactivateEmployee(employeeId);
        capabilityService.getByEmployee(employeeId)
            .forEach(c -> capabilityService.removeCapability(employeeId, c.getServiceId()));
    }
}
