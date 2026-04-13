package com.asaf.appointment_service.controller.employee;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.employee.EmployeeInvitePreviewResponse;
import com.asaf.appointment_service.service.employee.EmployeeInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/employee-invite")
public class PublicEmployeeInviteController {

    private final EmployeeInviteService inviteService;

    public PublicEmployeeInviteController(EmployeeInviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<EmployeeInvitePreviewResponse>> preview(
            @RequestParam String token) {
        EmployeeInvitePreviewResponse body = inviteService.getPreviewByToken(token);
        return ResponseEntity.ok(ApiResponse.success(body));
    }
}
