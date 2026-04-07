package com.asaf.appointment_service.dto.auth;

import com.asaf.appointment_service.entity.user.UserRole;

public record AuthResponse(
    String accessToken,
    String tokenType,
    Long expiresIn,
    UserInfo user
) {
    public static AuthResponse of(String accessToken, long expiresIn, UserInfo user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }

    public record UserInfo(
        Long id,
        String email,
        String name,
        Long businessId,
        UserRole role
    ) {}
}
