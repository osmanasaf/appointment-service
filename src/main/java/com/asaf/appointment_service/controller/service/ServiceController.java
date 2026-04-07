package com.asaf.appointment_service.controller.service;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.service.CreateServiceRequest;
import com.asaf.appointment_service.dto.service.ServiceResponse;
import com.asaf.appointment_service.dto.service.UpdateServiceRequest;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.mapper.service.ServiceMapper;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/services")
public class ServiceController {

    private final ServiceDefinitionService serviceDefinitionService;

    public ServiceController(ServiceDefinitionService serviceDefinitionService) {
        this.serviceDefinitionService = serviceDefinitionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @Valid @RequestBody CreateServiceRequest request) {
        ServiceDefinition service = serviceDefinitionService.createService(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(ServiceMapper.toResponse(service), "Hizmet oluşturuldu"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getService(@PathVariable Long id) {
        ServiceDefinition service = serviceDefinitionService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(ServiceMapper.toResponse(service)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {
        ServiceDefinition service = serviceDefinitionService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.success(ServiceMapper.toResponse(service), "Hizmet güncellendi"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServicesByBusiness(
            @RequestParam Long businessId,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) Boolean packageEligible) {
        List<ServiceDefinition> services;
        if (Boolean.TRUE.equals(packageEligible)) {
            services = serviceDefinitionService.findPackageEligibleByBusinessId(businessId);
            if (activeOnly) {
                services = services.stream().filter(ServiceDefinition::isActive).toList();
            }
        } else if (activeOnly) {
            services = serviceDefinitionService.findActiveByBusinessId(businessId);
        } else {
            services = serviceDefinitionService.findByBusinessId(businessId);
        }
        List<ServiceResponse> responses = services.stream()
            .map(ServiceMapper::toResponse)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ServiceResponse>> activateService(@PathVariable Long id) {
        ServiceDefinition service = serviceDefinitionService.activateService(id);
        return ResponseEntity.ok(ApiResponse.success(ServiceMapper.toResponse(service), "Hizmet aktifleştirildi"));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ServiceResponse>> deactivateService(@PathVariable Long id) {
        ServiceDefinition service = serviceDefinitionService.deactivateService(id);
        return ResponseEntity.ok(ApiResponse.success(ServiceMapper.toResponse(service), "Hizmet pasifleştirildi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        serviceDefinitionService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Hizmet silindi"));
    }
}
