package com.asaf.appointment_service.controller.plan;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.plan.AdminAssignPlanRequest;
import com.asaf.appointment_service.dto.plan.BusinessSubscriptionResponse;
import com.asaf.appointment_service.mapper.plan.PlanMapper;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/businesses")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    public AdminSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/{id}/plan")
    public ResponseEntity<ApiResponse<BusinessSubscriptionResponse>> assignPlan(
            @PathVariable Long id,
            @Valid @RequestBody AdminAssignPlanRequest request) {
        var subscription = subscriptionService.assignPlanToBusiness(id, request.planCode(), true);
        var plan = subscriptionService.getCurrentPlanByBusinessId(id);
        return ResponseEntity.ok(
            ApiResponse.success(
                PlanMapper.toResponse(subscription, plan),
                "İşletme planı güncellendi"
            )
        );
    }
}
