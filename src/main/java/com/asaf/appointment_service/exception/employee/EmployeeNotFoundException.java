package com.asaf.appointment_service.exception.employee;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class EmployeeNotFoundException extends EntityNotFoundException {

    public EmployeeNotFoundException(Long id) {
        super("Çalışan", id);
    }
}
