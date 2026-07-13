package com.asaf.appointment_service.mapper.plan;

import com.asaf.appointment_service.dto.plan.PublicPlanResponse;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanMapperTest {

    private static final PayTRProperties.Discount DEFAULT_DISCOUNT =
            new PayTRProperties.Discount(new BigDecimal("0.05"), new BigDecimal("0.10"));

    @Test
    void should_mapAllFieldsAndLocalizedFeatureNames_when_toPublicResponseCalled() {
        Plan plan = Plan.builder()
            .code(PlanCode.PRO)
            .maxEmployees(5)
            .maxAppointmentsMonthly(500)
            .maxServices(20)
            .maxBranches(1)
            .maxWhatsappMonthly(200)
            .maxSmsMonthly(100)
            .features(List.of(
                PlanFeature.WHATSAPP_NOTIFICATIONS,
                PlanFeature.PACKAGE_MANAGEMENT,
                PlanFeature.ONLINE_BOOKING))
            .monthlyPrice(new BigDecimal("1190.00"))
            .yearlyPrice(new BigDecimal("11900.00"))
            .build();

        PublicPlanResponse response = PlanMapper.toPublicResponse(plan, DEFAULT_DISCOUNT);

        assertThat(response.code()).isEqualTo(PlanCode.PRO);
        assertThat(response.monthlyPrice()).isEqualByComparingTo("1190.00");
        assertThat(response.yearlyPrice()).isEqualByComparingTo("11900.00");
        assertThat(response.maxEmployees()).isEqualTo(5);
        assertThat(response.maxAppointmentsMonthly()).isEqualTo(500);
        assertThat(response.maxServices()).isEqualTo(20);
        assertThat(response.maxBranches()).isEqualTo(1);
        assertThat(response.maxWhatsappMonthly()).isEqualTo(200);
        assertThat(response.maxSmsMonthly()).isEqualTo(100);
        assertThat(response.unlimitedEmployees()).isFalse();
        assertThat(response.unlimitedWhatsapp()).isFalse();
        assertThat(response.features()).containsExactlyInAnyOrder(
            PlanFeature.WHATSAPP_NOTIFICATIONS,
            PlanFeature.PACKAGE_MANAGEMENT,
            PlanFeature.ONLINE_BOOKING);
        assertThat(response.featureNames())
            .containsEntry(PlanFeature.WHATSAPP_NOTIFICATIONS, "WhatsApp bildirimleri")
            .containsEntry(PlanFeature.PACKAGE_MANAGEMENT, "Paket yönetimi")
            .containsEntry(PlanFeature.ONLINE_BOOKING, "Online randevu sayfası");
    }

    @Test
    void should_markUnlimitedFlags_when_planHasNegativeLimits() {
        Plan plan = Plan.builder()
            .code(PlanCode.ENTERPRISE)
            .maxEmployees(-1)
            .maxAppointmentsMonthly(-1)
            .maxServices(-1)
            .maxBranches(-1)
            .maxWhatsappMonthly(-1)
            .maxSmsMonthly(-1)
            .features(List.of())
            .monthlyPrice(new BigDecimal("1990.00"))
            .yearlyPrice(new BigDecimal("19900.00"))
            .build();

        PublicPlanResponse response = PlanMapper.toPublicResponse(plan, DEFAULT_DISCOUNT);

        assertThat(response.unlimitedEmployees()).isTrue();
        assertThat(response.unlimitedAppointments()).isTrue();
        assertThat(response.unlimitedServices()).isTrue();
        assertThat(response.unlimitedBranches()).isTrue();
        assertThat(response.unlimitedWhatsapp()).isTrue();
        assertThat(response.unlimitedSms()).isTrue();
        assertThat(response.features()).isEmpty();
        assertThat(response.featureNames()).isEmpty();
    }
}
