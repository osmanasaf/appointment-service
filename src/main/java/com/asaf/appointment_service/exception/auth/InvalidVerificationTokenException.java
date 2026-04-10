package com.asaf.appointment_service.exception.auth;

import com.asaf.appointment_service.common.exception.BusinessException;

public class InvalidVerificationTokenException extends BusinessException {

    public InvalidVerificationTokenException(String message) {
        super("INVALID_VERIFICATION_TOKEN", message);
    }
}
