package com.asaf.appointment_service.controller.dashboard;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.AuthenticatedBusinessService;
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
    private final AuthenticatedBusinessService authenticatedBusinessService;

    public DashboardController(DashboardService dashboardService,
                                AuthenticatedBusinessService authenticatedBusinessService) {
        this.dashboardService = dashboardService;
        this.authenticatedBusinessService = authenticatedBusinessService;
    }

    /**
     * Genel randevu istatistikleri.
     * GET /api/v1/dashboard/stats?period=DAILY|WEEKLY|MONTHLY
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestParam(defaultValue = "WEEKLY") DashboardPeriod period) {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        DashboardStatsResponse stats = dashboardService.getStats(businessId, period);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * En çok randevu alan hizmetler.
     * GET /api/v1/dashboard/top-services?period=MONTHLY&limit=5
     */
    @GetMapping("/top-services")
    public ResponseEntity<ApiResponse<TopServicesResponse>> getTopServices(
            @RequestParam(defaultValue = "MONTHLY") DashboardPeriod period,
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 20) limit = 5;
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        TopServicesResponse response = dashboardService.getTopServices(businessId, period, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Müşteri istatistikleri (yeni/tekrar eden, riskli, kara liste).
     * GET /api/v1/dashboard/customer-stats?period=MONTHLY
     */
    @GetMapping("/customer-stats")
    public ResponseEntity<ApiResponse<CustomerStatsResponse>> getCustomerStats(
            @RequestParam(defaultValue = "MONTHLY") DashboardPeriod period) {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        CustomerStatsResponse response = dashboardService.getCustomerStats(businessId, period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Paket kullanım durumu (aktif, düşük seans, bitmek üzere).
     * GET /api/v1/dashboard/package-usage
     */
    @GetMapping("/package-usage")
    public ResponseEntity<ApiResponse<PackageUsageResponse>> getPackageUsage() {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        PackageUsageResponse response = dashboardService.getPackageUsage(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
