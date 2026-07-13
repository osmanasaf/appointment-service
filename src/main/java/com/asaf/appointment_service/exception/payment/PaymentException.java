package com.asaf.appointment_service.exception.payment;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.common.exception.ErrorCode;

public class PaymentException extends BusinessException {

    public PaymentException(ErrorCode code) {
        super(code.getCode(), code.getDefaultMessage());
    }

    public PaymentException(ErrorCode code, String message) {
        super(code.getCode(), message);
    }
}
