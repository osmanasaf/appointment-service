package com.asaf.appointment_service.dto.employee;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EmployeeRequestDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    @Test
    void shouldApplyDefaultsWhenCreateEmployeeOptionalBooleansAreMissing() throws Exception {
        String json = """
            {
              "name": "Ayse Calisan",
              "title": "Kuafor"
            }
            """;

        CreateEmployeeRequest request = objectMapper.readValue(json, CreateEmployeeRequest.class);

        assertThat(request.name()).isEqualTo("Ayse Calisan");
        assertThat(request.phoneCountryCode()).isEqualTo("+90");
        assertThat(request.title()).isEqualTo("Kuafor");
        assertThat(request.owner()).isFalse();
    }

    @Test
    void shouldApplyDefaultsWhenUpdateEmployeeOptionalBooleansAreMissing() throws Exception {
        String json = """
            {
              "name": "Ayse Calisan"
            }
            """;

        UpdateEmployeeProfileRequest request = objectMapper.readValue(json, UpdateEmployeeProfileRequest.class);

        assertThat(request.name()).isEqualTo("Ayse Calisan");
        assertThat(request.acceptsOnlineBooking()).isTrue();
    }
}
