package com.asaf.appointment_service.controller.plan;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.AuthenticatedBusinessService;
import com.asaf.appointment_service.dto.plan.BusinessSubscriptionResponse;
import com.asaf.appointment_service.dto.plan.PlanUsageResponse;
import com.asaf.appointment_service.mapper.plan.PlanMapper;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/subscription")
public class MySubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthenticatedBusinessService authenticatedBusinessService;

    public MySubscriptionController(SubscriptionService subscriptionService,
                                    AuthenticatedBusinessService authenticatedBusinessService) {
        this.subscriptionService = subscriptionService;
        this.authenticatedBusinessService = authenticatedBusinessService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BusinessSubscriptionResponse>> getMySubscription() {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        var subscription = subscriptionService.getCurrentSubscriptionByBusinessId(businessId);
        var plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        return ResponseEntity.ok(ApiResponse.success(PlanMapper.toResponse(subscription, plan)));
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<PlanUsageResponse>> getMyUsage() {
        Long businessId = authenticatedBusinessService.getCurrentBusinessId();
        var usage = subscriptionService.getCurrentUsage(businessId);
        var plan = subscriptionService.getCurrentPlanByBusinessId(businessId);
        return ResponseEntity.ok(ApiResponse.success(PlanMapper.toUsageResponse(usage, plan)));
    }
}
