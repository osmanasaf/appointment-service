package com.asaf.appointment_service.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asaf.appointment_service.exception.token.ActionTokenExpiredException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerActionTokenTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/action-token-expired")
        void expired() {
            throw new ActionTokenExpiredException();
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new ProbeController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    void should_returnGoneWithActionTokenExpiredCode_when_tokenExpiredOrUsed() throws Exception {
        mockMvc.perform(get("/probe/action-token-expired"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ACTION_TOKEN_EXPIRED"));
    }
}
