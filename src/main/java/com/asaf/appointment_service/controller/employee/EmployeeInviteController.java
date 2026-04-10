package com.asaf.appointment_service.controller.employee;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.employee.CreateEmployeeInviteRequest;
import com.asaf.appointment_service.dto.employee.EmployeeInviteResponse;
import com.asaf.appointment_service.security.ratelimit.RateLimited;
import com.asaf.appointment_service.security.ratelimit.RateLimitType;
import com.asaf.appointment_service.service.employee.EmployeeInviteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee-invites")
public class EmployeeInviteController {

    private final EmployeeInviteService inviteService;
    private final CurrentUserService currentUserService;

    public EmployeeInviteController(EmployeeInviteService inviteService,
                                     CurrentUserService currentUserService) {
        this.inviteService = inviteService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @RateLimited(type = RateLimitType.INVITE)
    public ResponseEntity<ApiResponse<EmployeeInviteResponse>> createInvite(
            @Valid @RequestBody CreateEmployeeInviteRequest request) {
        // Sadece işletme sahibi davet oluşturabilir
        currentUserService.requireBusinessOwnerOrAdmin();
        Long businessId = currentUserService.getCurrentBusinessId();
        
        EmployeeInviteResponse response = inviteService.createInvite(businessId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Davet oluşturuldu"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeInviteResponse>>> listInvites() {
        // Sadece işletme sahibi davetleri görebilir
        currentUserService.requireBusinessOwnerOrAdmin();
        Long businessId = currentUserService.getCurrentBusinessId();
        
        List<EmployeeInviteResponse> invites = inviteService.listInvites(businessId);
        return ResponseEntity.ok(ApiResponse.success(invites));
    }

    @DeleteMapping("/{inviteId}")
    public ResponseEntity<ApiResponse<Void>> cancelInvite(@PathVariable Long inviteId) {
        // Sadece işletme sahibi daveti iptal edebilir
        currentUserService.requireBusinessOwnerOrAdmin();
        Long businessId = currentUserService.getCurrentBusinessId();
        
        inviteService.cancelInvite(businessId, inviteId);
        return ResponseEntity.ok(ApiResponse.success(null, "Davet iptal edildi"));
    }
}
