package com.asaf.appointment_service.exception.appointment;

import com.asaf.appointment_service.common.exception.BusinessException;

public class SlotNotAvailableException extends BusinessException {

    public SlotNotAvailableException(String message) {
        super("SLOT_NOT_AVAILABLE", message);
    }

    public SlotNotAvailableException() {
        super("SLOT_NOT_AVAILABLE", "Seçilen saat dilimi müsait değil");
    }
}
