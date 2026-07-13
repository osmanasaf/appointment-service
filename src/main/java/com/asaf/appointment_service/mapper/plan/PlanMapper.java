package com.asaf.appointment_service.mapper.plan;

import com.asaf.appointment_service.dto.plan.BusinessSubscriptionResponse;
import com.asaf.appointment_service.dto.plan.PlanResponse;
import com.asaf.appointment_service.dto.plan.PlanUsageResponse;
import com.asaf.appointment_service.dto.plan.PublicPlanResponse;
import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanFeatureNames;
import com.asaf.appointment_service.entity.plan.PlanLimitUsage;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

public class PlanMapper {

    private PlanMapper() {}

    public static PlanResponse toResponse(Plan plan, PayTRProperties.Discount discount) {
        Map<String, String> featureNames = plan.getFeatures().stream()
            .collect(Collectors.toMap(
                feature -> feature,
                PlanFeatureNames::getDisplayName
            ));

        return new PlanResponse(
            plan.getId(),
            plan.getCode(),
            plan.getMaxEmployees(),
            plan.getMaxAppointmentsMonthly(),
            plan.getMaxServices(),
            plan.getMaxBranches(),
            plan.getFeatures(),
            featureNames,
            plan.getMonthlyPrice(),
            packagePrice(plan.getMonthlyPrice(), BillingPeriod.QUARTERLY, discount.quarterly()),
            packagePrice(plan.getMonthlyPrice(), BillingPeriod.SEMIANNUAL, discount.semiannual()),
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

    public static PublicPlanResponse toPublicResponse(Plan plan, PayTRProperties.Discount discount) {
        Map<String, String> featureNames = plan.getFeatures().stream()
            .collect(Collectors.toMap(
                feature -> feature,
                PlanFeatureNames::getDisplayName
            ));

        return new PublicPlanResponse(
            plan.getCode(),
            plan.getMonthlyPrice(),
            packagePrice(plan.getMonthlyPrice(), BillingPeriod.QUARTERLY, discount.quarterly()),
            packagePrice(plan.getMonthlyPrice(), BillingPeriod.SEMIANNUAL, discount.semiannual()),
            plan.getYearlyPrice(),
            plan.getMaxEmployees(),
            plan.getMaxAppointmentsMonthly(),
            plan.getMaxServices(),
            plan.getMaxBranches(),
            plan.getMaxWhatsappMonthly(),
            plan.getMaxSmsMonthly(),
            plan.isUnlimitedEmployees(),
            plan.isUnlimitedAppointments(),
            plan.isUnlimitedServices(),
            plan.isUnlimitedBranches(),
            plan.isUnlimitedWhatsapp(),
            plan.isUnlimitedSms(),
            plan.getFeatures(),
            featureNames
        );
    }

    public static PlanUsageResponse toUsageResponse(PlanLimitUsage usage, Plan plan) {
        return new PlanUsageResponse(
            usage.getYear(),
            usage.getMonth(),
            usage.getAppointmentsCount(),
            usage.getSmsSent(),
            usage.getWhatsappSent(),
            plan.getMaxAppointmentsMonthly(),
            plan.getMaxSmsMonthly(),
            plan.getMaxWhatsappMonthly()
        );
    }

    private static BigDecimal packagePrice(BigDecimal monthlyPrice, BillingPeriod period, BigDecimal discountRate) {
        BigDecimal fullPrice = monthlyPrice.multiply(BigDecimal.valueOf(period.getMonths()));
        BigDecimal multiplier = BigDecimal.ONE.subtract(discountRate);
        return fullPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
