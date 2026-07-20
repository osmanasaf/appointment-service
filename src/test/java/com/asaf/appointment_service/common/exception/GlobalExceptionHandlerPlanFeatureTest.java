package com.asaf.appointment_service.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asaf.appointment_service.exception.plan.PlanFeatureNotAvailableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerPlanFeatureTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/plan-feature")
        void featureNotAvailable() {
            throw new PlanFeatureNotAvailableException("WAITLIST");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new ProbeController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    void should_returnPaymentRequiredWithFeatureCode_when_planFeatureNotAvailable() throws Exception {
        mockMvc.perform(get("/probe/plan-feature"))
            .andExpect(status().isPaymentRequired())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_AVAILABLE"))
            .andExpect(jsonPath("$.error.message").value("Bu özellik mevcut planınızda yok: WAITLIST. Planınızı yükseltin."));
    }
}
