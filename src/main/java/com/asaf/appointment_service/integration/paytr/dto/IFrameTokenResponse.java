package com.asaf.appointment_service.integration.paytr.dto;

public record IFrameTokenResponse(
    String token,
    boolean success,
    String errorMessage
) {

    public static IFrameTokenResponse success(String token) {
        return new IFrameTokenResponse(token, true, null);
    }

    public static IFrameTokenResponse failure(String errorMessage) {
        return new IFrameTokenResponse(null, false, errorMessage);
    }
}
