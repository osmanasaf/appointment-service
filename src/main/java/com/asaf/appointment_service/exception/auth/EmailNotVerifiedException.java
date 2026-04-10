package com.asaf.appointment_service.exception.auth;

import com.asaf.appointment_service.common.exception.BusinessException;

public class EmailNotVerifiedException extends BusinessException {

    public EmailNotVerifiedException() {
        super("EMAIL_NOT_VERIFIED", "Email adresiniz henüz doğrulanmamış. Lütfen gelen kutunuzu kontrol edin.");
    }
}
