package com.asaf.appointment_service.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerBadCredentialsTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("ignored");
        }

        @GetMapping("/probe/disabled")
        void disabled() {
            throw new DisabledException("ignored");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new ProbeController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    void should_return401WithInvalidCredentialsCode_when_badCredentials() throws Exception {
        mockMvc.perform(get("/probe/bad-credentials"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void should_return403WithAccountDisabledCode_when_disabled() throws Exception {
        mockMvc.perform(get("/probe/disabled"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
    }
}
