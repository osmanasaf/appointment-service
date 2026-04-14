package com.asaf.appointment_service.dto.auth;

import com.asaf.appointment_service.entity.user.UserRole;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    UserInfo user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }

    public record UserInfo(
        Long id,
        String email,
        String name,
        Long businessId,
        Long employeeId,
        UserRole role,
        String businessSlug
    ) {}
}
