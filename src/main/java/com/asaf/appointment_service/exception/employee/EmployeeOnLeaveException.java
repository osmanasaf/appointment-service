package com.asaf.appointment_service.exception.employee;

import com.asaf.appointment_service.common.exception.BusinessException;

public class EmployeeOnLeaveException extends BusinessException {

    public EmployeeOnLeaveException() {
        super("EMPLOYEE_ON_LEAVE", "Çalışan seçilen tarihte izinli olduğu için randevu alınamaz");
    }
}
