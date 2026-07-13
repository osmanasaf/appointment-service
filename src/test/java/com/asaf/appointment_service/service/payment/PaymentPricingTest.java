package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPricingTest {

    private static final BigDecimal QUARTERLY_DISCOUNT = new BigDecimal("0.05");
    private static final BigDecimal SEMIANNUAL_DISCOUNT = new BigDecimal("0.10");

    private final Plan plan = Plan.builder()
            .code(PlanCode.PRO)
            .maxEmployees(5)
            .maxAppointmentsMonthly(500)
            .maxServices(20)
            .maxBranches(1)
            .maxWhatsappMonthly(200)
            .maxSmsMonthly(100)
            .monthlyPrice(new BigDecimal("1000.00"))
            .yearlyPrice(new BigDecimal("11000.00"))
            .build();

    @Test
    void should_returnMonthlyPrice_when_periodIsMonthly() {
        BigDecimal amount = PaymentPricing.resolveAmount(plan, BillingPeriod.MONTHLY,
                QUARTERLY_DISCOUNT, SEMIANNUAL_DISCOUNT);

        assertThat(amount).isEqualByComparingTo("1000.00");
    }

    @Test
    void should_returnYearlyPrice_when_periodIsYearly() {
        BigDecimal amount = PaymentPricing.resolveAmount(plan, BillingPeriod.YEARLY,
                QUARTERLY_DISCOUNT, SEMIANNUAL_DISCOUNT);

        assertThat(amount).isEqualByComparingTo("11000.00");
    }

    @Test
    void should_apply5PercentDiscount_when_periodIsQuarterly() {
        BigDecimal amount = PaymentPricing.resolveAmount(plan, BillingPeriod.QUARTERLY,
                QUARTERLY_DISCOUNT, SEMIANNUAL_DISCOUNT);

        // 1000 * 3 = 3000, %5 indirim -> 2850.00
        assertThat(amount).isEqualByComparingTo("2850.00");
    }

    @Test
    void should_apply10PercentDiscount_when_periodIsSemiannual() {
        BigDecimal amount = PaymentPricing.resolveAmount(plan, BillingPeriod.SEMIANNUAL,
                QUARTERLY_DISCOUNT, SEMIANNUAL_DISCOUNT);

        // 1000 * 6 = 6000, %10 indirim -> 5400.00
        assertThat(amount).isEqualByComparingTo("5400.00");
    }

    @Test
    void should_addOneMonthMinusOneDay_when_periodIsMonthly() {
        LocalDate start = LocalDate.of(2026, 1, 15);

        LocalDate end = PaymentPricing.resolvePeriodEnd(start, BillingPeriod.MONTHLY);

        assertThat(end).isEqualTo(LocalDate.of(2026, 2, 14));
    }

    @Test
    void should_addSixMonthsMinusOneDay_when_periodIsSemiannual() {
        LocalDate start = LocalDate.of(2026, 1, 15);

        LocalDate end = PaymentPricing.resolvePeriodEnd(start, BillingPeriod.SEMIANNUAL);

        assertThat(end).isEqualTo(LocalDate.of(2026, 7, 14));
    }

    @Test
    void should_addTwelveMonthsMinusOneDay_when_periodIsYearly() {
        LocalDate start = LocalDate.of(2026, 1, 15);

        LocalDate end = PaymentPricing.resolvePeriodEnd(start, BillingPeriod.YEARLY);

        assertThat(end).isEqualTo(LocalDate.of(2027, 1, 14));
    }
}
