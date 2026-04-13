package com.asaf.appointment_service.controller.dashboard;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.dashboard.CustomerStatsResponse;
import com.asaf.appointment_service.dto.dashboard.DashboardPeriod;
import com.asaf.appointment_service.dto.dashboard.DashboardStatsResponse;
import com.asaf.appointment_service.dto.dashboard.PackageUsageResponse;
import com.asaf.appointment_service.dto.dashboard.TopServicesResponse;
import com.asaf.appointment_service.service.dashboard.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService,
                                CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestParam(defaultValue = "WEEKLY") DashboardPeriod period) {
        Long businessId = currentUserService.getCurrentBusinessId();
        DashboardStatsResponse stats = dashboardService.getStats(businessId, period);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/top-services")
    public ResponseEntity<ApiResponse<TopServicesResponse>> getTopServices(
            @RequestParam(defaultValue = "MONTHLY") DashboardPeriod period,
            @RequestParam(defaultValue = "5") int limit) {
        Long businessId = currentUserService.getCurrentBusinessId();
        TopServicesResponse response = dashboardService.getTopServices(businessId, period, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer-stats")
    public ResponseEntity<ApiResponse<CustomerStatsResponse>> getCustomerStats(
            @RequestParam(defaultValue = "MONTHLY") DashboardPeriod period) {
        Long businessId = currentUserService.getCurrentBusinessId();
        CustomerStatsResponse response = dashboardService.getCustomerStats(businessId, period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/package-usage")
    public ResponseEntity<ApiResponse<PackageUsageResponse>> getPackageUsage() {
        Long businessId = currentUserService.getCurrentBusinessId();
        PackageUsageResponse response = dashboardService.getPackageUsage(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
