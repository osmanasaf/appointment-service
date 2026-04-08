package com.asaf.appointment_service.controller.business;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.business.BusinessCategoryResponse;
import com.asaf.appointment_service.dto.business.BusinessResponse;
import com.asaf.appointment_service.dto.business.CreateBusinessRequest;
import com.asaf.appointment_service.dto.business.UpdateBusinessRequest;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.mapper.business.BusinessMapper;
import com.asaf.appointment_service.service.business.BusinessService;
import jakarta.validation.Valid;
import java.util.Arrays;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessResponse>> createBusiness(
            @Valid @RequestBody CreateBusinessRequest request) {
        Business business = businessService.createBusiness(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(BusinessMapper.toResponse(business), "İşletme başarıyla oluşturuldu"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> getBusiness(@PathVariable Long id) {
        Business business = businessService.findByIdOrThrow(id);
        return ResponseEntity.ok(ApiResponse.success(BusinessMapper.toResponse(business)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> updateBusiness(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBusinessRequest request) {
        Business business = businessService.updateBusiness(id, request);
        return ResponseEntity.ok(ApiResponse.success(BusinessMapper.toResponse(business), "İşletme güncellendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBusiness(@PathVariable Long id) {
        businessService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "İşletme silindi"));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<BusinessResponse>> getBusinessBySlug(@PathVariable String slug) {
        Business business = businessService.findBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(BusinessMapper.toResponse(business)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<BusinessCategoryResponse>>> getCategories() {
        List<BusinessCategoryResponse> categories = Arrays.stream(BusinessCategory.values())
            .map(BusinessCategoryResponse::from)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
