package com.asaf.appointment_service.controller.report;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.report.ReportDetailedResponse;
import com.asaf.appointment_service.dto.report.ReportSummaryResponse;
import com.asaf.appointment_service.service.report.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    public ReportController(ReportService reportService, CurrentUserService currentUserService) {
        this.reportService = reportService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary() {
        Long businessId = currentUserService.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary(businessId)));
    }

    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<ReportDetailedResponse>> getDetailed(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long employeeId) {
        Long businessId = currentUserService.getCurrentBusinessId();
        ReportDetailedResponse response = reportService.getDetailed(businessId, startDate, endDate, employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
