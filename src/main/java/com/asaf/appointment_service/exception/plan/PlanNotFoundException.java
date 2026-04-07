package com.asaf.appointment_service.exception.plan;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import com.asaf.appointment_service.entity.plan.PlanCode;

public class PlanNotFoundException extends EntityNotFoundException {

    public PlanNotFoundException(PlanCode code) {
        super("Plan", code.name());
    }
}
