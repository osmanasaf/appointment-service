package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.plan.Plan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * PaymentService ve RecurringPaymentScheduler arasında paylaşılan fiyat/dönem hesaplama mantığı.
 */
final class PaymentPricing {

    private PaymentPricing() {}

    static BigDecimal resolveAmount(Plan plan, BillingPeriod period,
                                    BigDecimal quarterlyDiscount, BigDecimal semiannualDiscount) {
        return switch (period) {
            case MONTHLY -> plan.getMonthlyPrice();
            case YEARLY -> plan.getYearlyPrice();
            case QUARTERLY -> applyDiscount(plan.getMonthlyPrice(), period, quarterlyDiscount);
            case SEMIANNUAL -> applyDiscount(plan.getMonthlyPrice(), period, semiannualDiscount);
        };
    }

    static LocalDate resolvePeriodEnd(LocalDate start, BillingPeriod period) {
        return start.plusMonths(period.getMonths()).minusDays(1);
    }

    private static BigDecimal applyDiscount(BigDecimal monthlyPrice, BillingPeriod period, BigDecimal discountRate) {
        BigDecimal fullPrice = monthlyPrice.multiply(BigDecimal.valueOf(period.getMonths()));
        BigDecimal multiplier = BigDecimal.ONE.subtract(discountRate);
        return fullPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
