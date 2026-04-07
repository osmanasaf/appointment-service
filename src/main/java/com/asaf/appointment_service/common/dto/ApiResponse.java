package com.asaf.appointment_service.common.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorDetails error,
    String message
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, null, message);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetails(code, message, null, LocalDateTime.now()), null);
    }

    public static <T> ApiResponse<T> error(ErrorDetails error) {
        return new ApiResponse<>(false, null, error, null);
    }

    public record ErrorDetails(
        String code,
        String message,
        Object details,
        LocalDateTime timestamp
    ) {
        public ErrorDetails(String code, String message) {
            this(code, message, null, LocalDateTime.now());
        }
    }
}
