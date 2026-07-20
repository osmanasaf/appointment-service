package com.asaf.appointment_service.entity.plan;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanTest {

    private Plan planWithFeatures(String... features) {
        return Plan.builder()
            .code(PlanCode.PRO)
            .maxEmployees(5)
            .maxAppointmentsMonthly(800)
            .maxServices(25)
            .maxBranches(1)
            .maxWhatsappMonthly(1500)
            .maxSmsMonthly(0)
            .features(List.of(features))
            .monthlyPrice(new BigDecimal("1190.00"))
            .yearlyPrice(new BigDecimal("11900.00"))
            .build();
    }

    @Test
    void should_addMissingCodes_when_mergeFeaturesCalledWithNewCodes() {
        Plan plan = planWithFeatures(PlanFeature.ONLINE_BOOKING);

        boolean changed = plan.mergeFeatures(List.of(PlanFeature.WAITLIST, PlanFeature.CUSTOMER_REVIEWS));

        assertThat(changed).isTrue();
        assertThat(plan.hasFeature(PlanFeature.ONLINE_BOOKING)).isTrue();
        assertThat(plan.hasFeature(PlanFeature.WAITLIST)).isTrue();
        assertThat(plan.hasFeature(PlanFeature.CUSTOMER_REVIEWS)).isTrue();
    }

    @Test
    void should_notDuplicate_when_mergeFeaturesCalledWithExistingCode() {
        Plan plan = planWithFeatures(PlanFeature.ONLINE_BOOKING, PlanFeature.WAITLIST);

        boolean changed = plan.mergeFeatures(List.of(PlanFeature.WAITLIST));

        assertThat(changed).isFalse();
        assertThat(plan.getFeatures()).containsExactly(PlanFeature.ONLINE_BOOKING, PlanFeature.WAITLIST);
    }

    @Test
    void should_returnFalse_when_allMergedCodesAlreadyPresent() {
        Plan plan = planWithFeatures(PlanFeature.WAITLIST, PlanFeature.CUSTOMER_REVIEWS);

        boolean changed = plan.mergeFeatures(List.of(PlanFeature.WAITLIST, PlanFeature.CUSTOMER_REVIEWS));

        assertThat(changed).isFalse();
    }

    @Test
    void should_notAddDuplicates_when_mergeFeaturesCalledWithRepeatedCodeInInput() {
        Plan plan = planWithFeatures(PlanFeature.ONLINE_BOOKING);

        boolean changed = plan.mergeFeatures(List.of(PlanFeature.WAITLIST, PlanFeature.WAITLIST));

        assertThat(changed).isTrue();
        assertThat(plan.getFeatures()).containsExactly(PlanFeature.ONLINE_BOOKING, PlanFeature.WAITLIST);
    }

    @Test
    void should_returnFalse_when_featureCodeNotInList() {
        Plan plan = planWithFeatures(PlanFeature.ONLINE_BOOKING);

        assertThat(plan.hasFeature(PlanFeature.WAITLIST)).isFalse();
    }
}
