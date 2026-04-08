package com.asaf.appointment_service.exception.employee;

import com.asaf.appointment_service.common.exception.BusinessException;

public class EmployeeCapabilityNotFoundException extends BusinessException {

    public EmployeeCapabilityNotFoundException(String employeeName, String serviceName) {
        super("CAPABILITY_NOT_FOUND",
              "\"%s\" adlı çalışan \"%s\" hizmetini sunamaz. Lütfen farklı bir çalışan seçin."
                  .formatted(employeeName, serviceName));
    }
}
