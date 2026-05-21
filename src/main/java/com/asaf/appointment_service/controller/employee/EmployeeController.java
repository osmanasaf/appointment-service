package com.asaf.appointment_service.controller.employee;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.dto.PageResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.employee.AffectedAppointmentResponse;
import com.asaf.appointment_service.dto.employee.AssignCapabilityRequest;
import com.asaf.appointment_service.dto.employee.CreateEmployeeFromInviteRequest;
import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.dto.employee.CreateEmployeeInviteRequest;
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
import com.asaf.appointment_service.service.employee.EmployeeInviteService;
import com.asaf.appointment_service.service.employee.EmployeeOffboardingService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import com.asaf.appointment_service.service.employee.EmployeeService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final EmployeeInviteService inviteService;
    private final EmployeeScheduleService scheduleService;
    private final EmployeeOffboardingService offboardingService;
    private final ServiceDefinitionService serviceDefinitionService;
    private final CurrentUserService currentUserService;

    public EmployeeController(EmployeeService employeeService,
                               EmployeeCapabilityService capabilityService,
                               EmployeeInviteService inviteService,
                               EmployeeScheduleService scheduleService,
                               EmployeeOffboardingService offboardingService,
                               ServiceDefinitionService serviceDefinitionService,
                               CurrentUserService currentUserService) {
        this.employeeService = employeeService;
        this.capabilityService = capabilityService;
        this.inviteService = inviteService;
        this.scheduleService = scheduleService;
        this.offboardingService = offboardingService;
        this.serviceDefinitionService = serviceDefinitionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        Long businessId = currentUserService.getCurrentBusinessId();
        Employee employee = employeeService.createEmployee(businessId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan oluşturuldu"));
    }

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployeeAndInvite(
            @Valid @RequestBody CreateEmployeeFromInviteRequest request) {
        currentUserService.requireBusinessOwnerOrAdmin();
        Long businessId = currentUserService.getCurrentBusinessId();
        Employee employee = employeeService.createEmployee(
            businessId,
            new CreateEmployeeRequest(
                request.name(),
                null,
                "+90",
                request.email(),
                request.message(),
                request.title(),
                false
            )
        );
        if (request.title() != null) {
            employee = employeeService.updateProfile(
                employee.getId(),
                new UpdateEmployeeProfileRequest(
                    employee.getName(),
                    request.title(),
                    employee.getEmail(),
                    employee.getBio(),
                    employee.getProfileImageUrl(),
                    employee.getExperienceYears(),
                    employee.isAcceptsOnlineBooking()
                )
            );
        }
        inviteService.createInvite(
            businessId,
            new CreateEmployeeInviteRequest(employee.getId(), request.email())
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(EmployeeMapper.toResponse(employee), "Çalışan davet edildi"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> getEmployee(@PathVariable Long id) {
        Employee employee = employeeService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toDetailResponse(employee)));
    }

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getEmployeesByBusiness(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long businessId = currentUserService.getCurrentBusinessId();
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, safeSize, Sort.by("name").ascending());
        if (activeOnly) {
            List<Employee> employees = employeeService.findActiveByBusinessId(businessId);
            return ResponseEntity.ok(ApiResponse.success(
                new PageResponse<>(employees.stream().map(EmployeeMapper::toResponse).toList(),
                    0, employees.size(), employees.size(), 1, false, false)));
        }
        return ResponseEntity.ok(ApiResponse.success(
            PageResponse.from(employeeService.findByBusinessIdPaged(businessId, pageable), EmployeeMapper::toResponse)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeProfileRequest request) {
        Employee employee = employeeService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success(EmployeeMapper.toDetailResponse(employee), "Çalışan güncellendi"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> patchEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeProfileRequest request) {
        return updateEmployee(id, request);
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
        List<Long> serviceIds = capabilities.stream()
            .map(EmployeeServiceCapability::getServiceId)
            .distinct()
            .toList();
        return serviceDefinitionService.findAllByIds(serviceIds).stream()
            .collect(Collectors.toMap(ServiceDefinition::getId, ServiceDefinition::getName));
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
