package com.asaf.appointment_service.dto.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.asaf.appointment_service.entity.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AuthResponseUserInfoSerializationTest {

    @Test
    void shouldIncludeBusinessSlugInJsonWhenSerialized() throws Exception {
        var userInfo = new AuthResponse.UserInfo(
            1L, "owner@example.com", "Owner", 42L, null, UserRole.BUSINESS_OWNER, "guvenlik-kamera");
        var response = AuthResponse.of("at", "rt", 3600L, userInfo);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("\"businessSlug\":\"guvenlik-kamera\"");
    }

    @Test
    void shouldIncludeNullBusinessSlugInJsonWhenAbsent() throws Exception {
        var userInfo = new AuthResponse.UserInfo(
            1L, "admin@example.com", "Admin", null, null, UserRole.ADMIN, null);
        var response = AuthResponse.of("at", "rt", 3600L, userInfo);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("\"businessSlug\":null");
    }
}
