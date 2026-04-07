package com.asaf.appointment_service.exception.customer;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class CustomerNotFoundException extends EntityNotFoundException {

    public CustomerNotFoundException(Long id) {
        super("Müşteri", id);
    }
}
