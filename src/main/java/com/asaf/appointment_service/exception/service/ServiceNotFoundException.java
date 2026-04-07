package com.asaf.appointment_service.exception.service;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class ServiceNotFoundException extends EntityNotFoundException {

    public ServiceNotFoundException(Long id) {
        super("Hizmet", id);
    }
}
