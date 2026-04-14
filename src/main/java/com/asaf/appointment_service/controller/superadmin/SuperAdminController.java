package com.asaf.appointment_service.controller.superadmin;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.superadmin.ChangePlanRequest;
import com.asaf.appointment_service.dto.superadmin.SuperAdminBusinessDetailResponse;
import com.asaf.appointment_service.dto.superadmin.SuperAdminBusinessListResponse;
import com.asaf.appointment_service.dto.superadmin.SuperAdminOverviewResponse;
import com.asaf.appointment_service.dto.superadmin.SuperAdminRevenueResponse;
import com.asaf.appointment_service.service.superadmin.SuperAdminBusinessService;
import com.asaf.appointment_service.service.superadmin.SuperAdminDashboardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SuperAdminDashboardService dashboardService;
    private final SuperAdminBusinessService businessService;

    public SuperAdminController(SuperAdminDashboardService dashboardService,
                                 SuperAdminBusinessService businessService) {
        this.dashboardService = dashboardService;
        this.businessService = businessService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<SuperAdminOverviewResponse>> getOverview() {
        SuperAdminOverviewResponse overview = dashboardService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<SuperAdminRevenueResponse>> getRevenue() {
        SuperAdminRevenueResponse revenue = dashboardService.getRevenue();
        return ResponseEntity.ok(ApiResponse.success(revenue));
    }

    @GetMapping("/businesses")
    public ResponseEntity<ApiResponse<Page<SuperAdminBusinessListResponse>>> listBusinesses(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SuperAdminBusinessListResponse> result = businessService.listBusinesses(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/businesses/{id}")
    public ResponseEntity<ApiResponse<SuperAdminBusinessDetailResponse>> getBusinessDetail(
            @PathVariable Long id) {
        SuperAdminBusinessDetailResponse detail = businessService.getBusinessDetail(id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PutMapping("/businesses/{id}/plan")
    public ResponseEntity<ApiResponse<Void>> changePlan(
            @PathVariable Long id,
            @Valid @RequestBody ChangePlanRequest request) {
        businessService.changePlan(id, request.planCode(), request.periodMonths());
        return ResponseEntity.ok(ApiResponse.success(null, "Plan başarıyla değiştirildi"));
    }

    @PutMapping("/businesses/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendBusiness(@PathVariable Long id) {
        businessService.suspendBusiness(id);
        return ResponseEntity.ok(ApiResponse.success(null, "İşletme askıya alındı"));
    }

    @PutMapping("/businesses/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateBusiness(@PathVariable Long id) {
        businessService.activateBusiness(id);
        return ResponseEntity.ok(ApiResponse.success(null, "İşletme aktifleştirildi"));
    }
}
