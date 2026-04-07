package com.asaf.appointment_service.exception.business;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class BusinessNotFoundException extends EntityNotFoundException {

    public BusinessNotFoundException(Long id) {
        super("İşletme", id);
    }

    public BusinessNotFoundException(String slug) {
        super("İşletme", slug);
    }
}
