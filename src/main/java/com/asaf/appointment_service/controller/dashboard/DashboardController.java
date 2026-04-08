package com.asaf.appointment_service.controller.dashboard;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.dashboard.DashboardPeriod;
import com.asaf.appointment_service.dto.dashboard.DashboardStatsResponse;
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

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestParam Long businessId,
            @RequestParam(defaultValue = "DAILY") DashboardPeriod period) {
        DashboardStatsResponse stats = dashboardService.getStats(businessId, period);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
