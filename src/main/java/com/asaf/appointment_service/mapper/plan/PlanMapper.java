package com.asaf.appointment_service.mapper.plan;

import com.asaf.appointment_service.dto.plan.BusinessSubscriptionResponse;
import com.asaf.appointment_service.dto.plan.PlanResponse;
import com.asaf.appointment_service.dto.plan.PlanUsageResponse;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanLimitUsage;

public class PlanMapper {

    private PlanMapper() {}

    public static PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
            plan.getId(),
            plan.getCode(),
            plan.getMaxEmployees(),
            plan.getMaxAppointmentsMonthly(),
            plan.getMaxServices(),
            plan.getMaxBranches(),
            plan.getFeatures(),
            plan.getMonthlyPrice(),
            plan.getYearlyPrice()
        );
    }

    public static BusinessSubscriptionResponse toResponse(BusinessSubscription subscription, Plan plan) {
        return new BusinessSubscriptionResponse(
            subscription.getBusinessId(),
            plan.getCode(),
            subscription.getStatus(),
            subscription.getTrialEndDate(),
            subscription.getCurrentPeriodStart(),
            subscription.getCurrentPeriodEnd(),
            subscription.isManual()
        );
    }

    public static PlanUsageResponse toUsageResponse(PlanLimitUsage usage, Plan plan) {
        return new PlanUsageResponse(
            usage.getYear(),
            usage.getMonth(),
            usage.getAppointmentsCount(),
            usage.getSmsSent(),
            usage.getWhatsappSent(),
            plan.getMaxAppointmentsMonthly()
        );
    }
}
