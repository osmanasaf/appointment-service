package com.asaf.appointment_service.dto.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CreateServiceRequestDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    @Test
    void shouldApplyDefaultsWhenOptionalFieldsAreMissing() throws Exception {
        String json = """
            {
              "name": "Saç Kesimi",
              "durationMinutes": 30
            }
            """;

        CreateServiceRequest request = objectMapper.readValue(json, CreateServiceRequest.class);

        assertThat(request.name()).isEqualTo("Saç Kesimi");
        assertThat(request.durationMinutes()).isEqualTo(30);
        assertThat(request.bufferBeforeMinutes()).isZero();
        assertThat(request.bufferAfterMinutes()).isZero();
        assertThat(request.requiresConfirmation()).isFalse();
        assertThat(request.sameDayBookingAllowed()).isTrue();
        assertThat(request.packageEligible()).isTrue();
    }

    @Test
    void shouldApplyDefaultsToUpdateRequestWhenOptionalFieldsAreMissing() throws Exception {
        String json = """
            {
              "name": "Saç Kesimi",
              "durationMinutes": 30
            }
            """;

        UpdateServiceRequest request = objectMapper.readValue(json, UpdateServiceRequest.class);

        assertThat(request.bufferBeforeMinutes()).isZero();
        assertThat(request.bufferAfterMinutes()).isZero();
        assertThat(request.requiresConfirmation()).isFalse();
        assertThat(request.sameDayBookingAllowed()).isTrue();
        assertThat(request.packageEligible()).isTrue();
    }
}
