package com.asaf.appointment_service.exception.employee;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;

public class EmployeeLeaveNotFoundException extends EntityNotFoundException {

    public EmployeeLeaveNotFoundException(Long id) {
        super("İzin", id);
    }
}
