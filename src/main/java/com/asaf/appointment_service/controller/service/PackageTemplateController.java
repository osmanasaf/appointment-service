package com.asaf.appointment_service.controller.service;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.service.CreatePackageTemplateRequest;
import com.asaf.appointment_service.dto.service.PackageTemplateResponse;
import com.asaf.appointment_service.dto.service.UpdatePackageTemplateRequest;
import com.asaf.appointment_service.entity.service.PackageTemplate;
import com.asaf.appointment_service.mapper.service.PackageTemplateMapper;
import com.asaf.appointment_service.service.service.PackageTemplateService;
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
@RequestMapping("/api/v1/package-templates")
public class PackageTemplateController {

    private final PackageTemplateService packageTemplateService;

    public PackageTemplateController(PackageTemplateService packageTemplateService) {
        this.packageTemplateService = packageTemplateService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PackageTemplateResponse>> create(
            @Valid @RequestBody CreatePackageTemplateRequest request) {
        PackageTemplate template = packageTemplateService.create(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(PackageTemplateMapper.toResponse(template), "Paket şablonu oluşturuldu"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PackageTemplateResponse>>> listByBusiness(
            @RequestParam Long businessId) {
        List<PackageTemplate> templates = packageTemplateService.listByBusiness(businessId);
        return ResponseEntity.ok(ApiResponse.success(PackageTemplateMapper.toResponseList(templates)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageTemplateResponse>> getById(@PathVariable Long id) {
        PackageTemplate template = packageTemplateService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(PackageTemplateMapper.toResponse(template)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PackageTemplateResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageTemplateRequest request) {
        PackageTemplate template = packageTemplateService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(PackageTemplateMapper.toResponse(template), "Paket şablonu güncellendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        packageTemplateService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Paket şablonu devre dışı bırakıldı"));
    }
}
