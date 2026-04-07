package com.asaf.appointment_service.exception.appointment;

import com.asaf.appointment_service.common.exception.BusinessException;

public class CustomerDayLimitExceededException extends BusinessException {

    public CustomerDayLimitExceededException(int limit, int currentCount) {
        super("CUSTOMER_DAY_LIMIT_EXCEEDED",
            String.format("Bu müşterinin seçilen günde zaten %d randevusu var. İşletme kuralına göre aynı güne en fazla %d randevu alınabilir.",
                currentCount, limit));
    }
}
