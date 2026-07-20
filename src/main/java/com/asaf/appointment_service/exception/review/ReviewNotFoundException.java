package com.asaf.appointment_service.exception.review;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class ReviewNotFoundException extends EntityNotFoundException {

    public ReviewNotFoundException(Long id) {
        super("Değerlendirme", id);
    }
}
