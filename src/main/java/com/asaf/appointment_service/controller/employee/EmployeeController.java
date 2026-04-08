package com.asaf.appointment_service.controller.employee;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.employee.AffectedAppointmentResponse;
import com.asaf.appointment_service.dto.employee.AssignCapabilityRequest;
import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.dto.employee.EmployeeCapabilityResponse;
import com.asaf.appointment_service.dto.employee.EmployeeDetailResponse;
import com.asaf.appointment_service.dto.employee.EmployeeResponse;
import com.asaf.appointment_service.dto.employee.EmployeeScheduleRequest;
import com.asaf.appointment_service.dto.employee.EmployeeScheduleResponse;
import com.asaf.appointment_service.dto.employee.OffboardRequest;
import com.asaf.appointment_service.dto.employee.OffboardSummaryResponse;
import com.asaf.appointment_service.dto.employee.UpdateEmployeeProfileRequest;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeServiceCapability;
import com.asaf.appointment_service.entity.employee.EmployeeWorkingSchedule;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.mapper.employee.EmployeeCapabilityMapper;
import com.asaf.appointment_service.mapper.employee.EmployeeMapper;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeOffboardingService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeCapabilityService capabilityService;
    private final EmployeeScheduleService scheduleService;
    private final EmployeeOffboardingService offboardingService;
    private final ServiceDefinitionService serviceDefinitionService;

    public EmployeeController(EmployeeService employeeService,
                               EmployeeCapabilityService capabilityService,
                               EmployeeScheduleService scheduleService,
                               EmployeeOffboardingService offboardingService,
                               ServiceDefinitionService serviceDefinitionService) {
        this.employeeService = employeeService;
        this.capabilityService = capabilityService;
        this.scheduleService = scheduleService;
        this.offboardingService = offboardingService;
        this.serviceDefinitionService = serviceDefinitionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        Employee employee = employeeService.createEmployee(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan oluşturuldu"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> getEmployee(@PathVariable Long id) {
        Employee employee = employeeService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toDetailResponse(employee)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesByBusiness(
            @RequestParam Long businessId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<Employee> employees = activeOnly
            ? employeeService.findActiveByBusinessId(businessId)
            : employeeService.findByBusinessId(businessId);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponseList(employees)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeProfileRequest request) {
        Employee employee = employeeService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toDetailResponse(employee), "Çalışan güncellendi"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activateEmployee(@PathVariable Long id) {
        Employee employee = employeeService.activateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan aktifleştirildi"));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivateEmployee(@PathVariable Long id) {
        Employee employee = employeeService.deactivateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan pasifleştirildi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Çalışan silindi"));
    }

    @PostMapping("/{id}/capabilities")
    public ResponseEntity<ApiResponse<EmployeeCapabilityResponse>> assignCapability(
            @PathVariable Long id,
            @Valid @RequestBody AssignCapabilityRequest request) {
        EmployeeServiceCapability capability = capabilityService.assignCapability(id, request);
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(capability.getServiceId());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                EmployeeCapabilityMapper.toResponse(capability, service.getName()),
                "Yetkinlik atandı"));
    }

    @DeleteMapping("/{id}/capabilities/{serviceId}")
    public ResponseEntity<ApiResponse<Void>> removeCapability(
            @PathVariable Long id,
            @PathVariable Long serviceId) {
        capabilityService.removeCapability(id, serviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Yetkinlik kaldırıldı"));
    }

    @GetMapping("/{id}/capabilities")
    public ResponseEntity<ApiResponse<List<EmployeeCapabilityResponse>>> getCapabilities(
            @PathVariable Long id) {
        List<EmployeeServiceCapability> capabilities = capabilityService.getByEmployee(id);
        Map<Long, String> serviceNames = resolveServiceNames(capabilities);
        return ResponseEntity.ok(ApiResponse.success(
            EmployeeCapabilityMapper.toResponseList(capabilities, serviceNames)));
    }

    private Map<Long, String> resolveServiceNames(List<EmployeeServiceCapability> capabilities) {
        return capabilities.stream()
            .collect(Collectors.toMap(
                EmployeeServiceCapability::getServiceId,
                c -> serviceDefinitionService.findByIdOrThrow(c.getServiceId()).getName(),
                (a, b) -> a
            ));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<List<EmployeeScheduleResponse>>> getSchedule(@PathVariable Long id) {
        List<EmployeeWorkingSchedule> schedule = scheduleService.getSchedule(id);
        List<EmployeeScheduleResponse> responses = schedule.stream()
            .map(EmployeeScheduleService::toResponse)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<EmployeeScheduleResponse>> setDaySchedule(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeScheduleRequest request) {
        EmployeeWorkingSchedule saved = scheduleService.setDaySchedule(id, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(EmployeeScheduleService.toResponse(saved), "Takvim güncellendi"));
    }

    @GetMapping("/{id}/offboard-preview")
    public ResponseEntity<ApiResponse<List<AffectedAppointmentResponse>>> offboardPreview(
            @PathVariable Long id) {
        List<AffectedAppointmentResponse> affected = offboardingService.previewOffboarding(id);
        return ResponseEntity.ok(ApiResponse.success(affected));
    }

    @PostMapping("/{id}/offboard")
    public ResponseEntity<ApiResponse<OffboardSummaryResponse>> offboard(
            @PathVariable Long id,
            @Valid @RequestBody OffboardRequest request) {
        OffboardSummaryResponse summary = offboardingService.offboard(id, request);
        return ResponseEntity.ok(ApiResponse.success(summary, "Çalışan offboard edildi"));
    }
}
