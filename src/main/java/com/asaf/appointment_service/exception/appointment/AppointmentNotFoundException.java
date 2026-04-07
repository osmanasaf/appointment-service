package com.asaf.appointment_service.exception.appointment;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class AppointmentNotFoundException extends EntityNotFoundException {

    public AppointmentNotFoundException(Long id) {
        super("Randevu", id);
    }
}
