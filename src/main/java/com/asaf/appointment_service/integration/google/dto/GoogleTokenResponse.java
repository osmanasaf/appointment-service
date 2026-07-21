package com.asaf.appointment_service.integration.google.dto;

public record GoogleTokenResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    boolean success,
    String errorMessage
) {
    public static GoogleTokenResponse success(String accessToken, String refreshToken, long expiresInSeconds) {
        return new GoogleTokenResponse(accessToken, refreshToken, expiresInSeconds, true, null);
    }

    public static GoogleTokenResponse failure(String errorMessage) {
        return new GoogleTokenResponse(null, null, 0, false, errorMessage);
    }
}
