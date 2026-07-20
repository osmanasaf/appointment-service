package com.asaf.appointment_service.exception.waitlist;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class WaitlistEntryNotFoundException extends EntityNotFoundException {

    public WaitlistEntryNotFoundException(Long id) {
        super("Bekleme listesi kaydı", id);
    }
}
