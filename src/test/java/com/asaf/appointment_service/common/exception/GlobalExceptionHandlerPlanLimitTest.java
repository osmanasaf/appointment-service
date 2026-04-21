package com.asaf.appointment_service.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asaf.appointment_service.exception.plan.PlanLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerPlanLimitTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/plan-limit")
        void limitExceeded() {
            throw new PlanLimitExceededException("Çalışan limitiniz doldu. Planınızı yükseltin.");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new ProbeController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    void should_returnPlanLimitMessageInTopLevelAndErrorBody_when_planLimitExceeded() throws Exception {
        mockMvc.perform(get("/probe/plan-limit"))
            .andExpect(status().isPaymentRequired())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("PLAN_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.error.message").value("Çalışan limitiniz doldu. Planınızı yükseltin."))
            .andExpect(jsonPath("$.message").value("Çalışan limitiniz doldu. Planınızı yükseltin."));
    }
}
