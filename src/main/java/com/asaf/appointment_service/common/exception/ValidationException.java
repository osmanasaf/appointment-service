package com.asaf.appointment_service.common.exception;

import java.util.List;

public class ValidationException extends BusinessException {

    private final List<FieldError> fieldErrors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = List.of();
    }

    public ValidationException(List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", "Girilen veriler geçersiz");
        this.fieldErrors = fieldErrors;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public record FieldError(String field, String message) {}
}
