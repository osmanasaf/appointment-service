package com.asaf.appointment_service.controller.plan;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.dto.plan.PlanResponse;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import com.asaf.appointment_service.mapper.plan.PlanMapper;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final SubscriptionService subscriptionService;
    private final PayTRProperties payTRProperties;

    public PlanController(SubscriptionService subscriptionService, PayTRProperties payTRProperties) {
        this.subscriptionService = subscriptionService;
        this.payTRProperties = payTRProperties;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlans() {
        List<PlanResponse> plans = subscriptionService.getPlans()
            .stream()
            .map(plan -> PlanMapper.toResponse(plan, payTRProperties.discount()))
            .toList();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }
}
