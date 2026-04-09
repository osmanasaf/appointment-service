package com.asaf.appointment_service.controller.dashboard;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.AuthenticatedBusinessService;
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
    private final AuthenticatedBusinessService authenticatedBusinessService;

    public DashboardController(DashboardService dashboardService,
                                AuthenticatedBusinessService authenticatedBusinessService) {
        this.dashboardService = dashboardService;
        this.authenticatedBusinessService = authenticatedBusinessService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestParam(defaultValue = "DAILY") DashboardPeriod period) {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        DashboardStatsResponse stats = dashboardService.getStats(businessId, period);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
