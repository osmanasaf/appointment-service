package com.asaf.appointment_service.exception.token;

import com.asaf.appointment_service.common.exception.BusinessException;

public class ActionTokenExpiredException extends BusinessException {

    public ActionTokenExpiredException() {
        super("ACTION_TOKEN_EXPIRED",
            "Bu bağlantının süresi dolmuş veya zaten kullanılmış. Lütfen işletmeyle iletişime geçin.");
    }
}
