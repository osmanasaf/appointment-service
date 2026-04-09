package com.asaf.appointment_service.controller.customer;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.AuthenticatedBusinessService;
import com.asaf.appointment_service.dto.customer.CreatePackageFromTemplateRequest;
import com.asaf.appointment_service.dto.customer.CreatePackageRequest;
import com.asaf.appointment_service.dto.customer.PackageResponse;
import com.asaf.appointment_service.dto.customer.PackageSessionResponse;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.customer.PackageSession;
import com.asaf.appointment_service.mapper.customer.CustomerMapper;
import com.asaf.appointment_service.mapper.customer.PackageSessionMapper;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.customer.PackageService;
import com.asaf.appointment_service.service.customer.PackageSessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/packages")
public class PackageController {

    private final PackageService packageService;
    private final PackageSessionService packageSessionService;
    private final AppointmentService appointmentService;
    private final AuthenticatedBusinessService authenticatedBusinessService;

    public PackageController(PackageService packageService,
                             PackageSessionService packageSessionService,
                             AppointmentService appointmentService,
                             AuthenticatedBusinessService authenticatedBusinessService) {
        this.packageService = packageService;
        this.packageSessionService = packageSessionService;
        this.appointmentService = appointmentService;
        this.authenticatedBusinessService = authenticatedBusinessService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PackageResponse>> createPackage(
            @Valid @RequestBody CreatePackageRequest request) {
        CustomerPackage customerPackage = packageService.createPackage(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(CustomerMapper.toPackageResponse(customerPackage), "Paket oluşturuldu"));
    }

    @PostMapping("/from-template")
    public ResponseEntity<ApiResponse<PackageResponse>> createFromTemplate(
            @Valid @RequestBody CreatePackageFromTemplateRequest request) {
        CustomerPackage customerPackage = packageService.createFromTemplate(request.customerId(), request.templateId());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(CustomerMapper.toPackageResponse(customerPackage), "Paket şablondan oluşturuldu"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PackageResponse>>> listPackages(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long templateId) {
        if (templateId != null) {
            Long businessId = authenticatedBusinessService.getCurrentBusinessId();
            List<CustomerPackage> packages = packageService.findByBusinessIdAndTemplateId(businessId, templateId);
            return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toPackageResponseList(packages)));
        }
        if (customerId != null) {
            List<CustomerPackage> packages = packageService.findActiveByCustomerId(customerId);
            return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toPackageResponseList(packages)));
        }
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("BAD_REQUEST", "customerId veya templateId parametresi gerekli"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageResponse>> getPackage(@PathVariable Long id) {
        CustomerPackage customerPackage = packageService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toPackageResponse(customerPackage)));
    }

    @PostMapping("/{id}/deduct")
    public ResponseEntity<ApiResponse<PackageResponse>> deductSession(@PathVariable Long id) {
        CustomerPackage customerPackage = packageService.deductSession(id);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toPackageResponse(customerPackage), 
            "Seans düşüldü. Kalan: " + customerPackage.getRemainingSessions()));
    }

    @PostMapping("/{id}/add-sessions")
    public ResponseEntity<ApiResponse<PackageResponse>> addSessions(
            @PathVariable Long id,
            @RequestParam int count) {
        CustomerPackage customerPackage = packageService.addSession(id, count);
        return ResponseEntity.ok(ApiResponse.success(CustomerMapper.toPackageResponse(customerPackage), 
            count + " seans eklendi"));
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<ApiResponse<List<PackageSessionResponse>>> getSessions(@PathVariable Long id) {
        packageService.findByIdOrThrow(id);
        List<PackageSession> sessions = packageSessionService.listByPackage(id);
        return ResponseEntity.ok(ApiResponse.success(PackageSessionMapper.toResponseList(sessions)));
    }

    @PutMapping("/{id}/sessions/{sessionId}/assign")
    public ResponseEntity<ApiResponse<PackageSessionResponse>> assignSession(
            @PathVariable Long id,
            @PathVariable Long sessionId,
            @Valid @RequestBody com.asaf.appointment_service.dto.customer.AssignSessionRequest request) {
        packageService.findByIdOrThrow(id);
        appointmentService.assignPackageSession(request.appointmentId(), sessionId);
        PackageSession session = packageSessionService.findByIdOrThrow(sessionId);
        return ResponseEntity.ok(ApiResponse.success(PackageSessionMapper.toResponse(session), "Seansa randevu atandı"));
    }

    @PutMapping("/{id}/sessions/{sessionId}/unassign")
    public ResponseEntity<ApiResponse<PackageSessionResponse>> unassignSession(
            @PathVariable Long id,
            @PathVariable Long sessionId) {
        packageService.findByIdOrThrow(id);
        PackageSession session = packageSessionService.findByIdOrThrow(sessionId);
        if (session.getAppointmentId() == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("NO_APPOINTMENT", "Bu seansa atanmış randevu yok"));
        }
        appointmentService.unassignPackageSession(session.getAppointmentId());
        PackageSession updated = packageSessionService.findByIdOrThrow(sessionId);
        return ResponseEntity.ok(ApiResponse.success(PackageSessionMapper.toResponse(updated), "Randevu seanstan kaldırıldı"));
    }
}
