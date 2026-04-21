package com.asaf.appointment_service.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void should_setTopLevelMessage_when_errorCreatedFromCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error("PLAN_LIMIT_EXCEEDED", "Çalışan limitiniz doldu.");

        assertThat(response.success()).isFalse();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo("PLAN_LIMIT_EXCEEDED");
        assertThat(response.message()).isEqualTo("Çalışan limitiniz doldu.");
    }

    @Test
    void should_copyErrorMessageToTopLevelMessage_when_errorDetailsProvided() {
        ApiResponse.ErrorDetails errorDetails = new ApiResponse.ErrorDetails(
            "PLAN_LIMIT_EXCEEDED",
            "Çalışan limitiniz doldu. Planınızı yükseltin.",
            null,
            LocalDateTime.now()
        );

        ApiResponse<Void> response = ApiResponse.error(errorDetails);

        assertThat(response.success()).isFalse();
        assertThat(response.error()).isNotNull();
        assertThat(response.message()).isEqualTo(errorDetails.message());
    }
}
