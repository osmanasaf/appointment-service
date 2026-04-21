package com.asaf.appointment_service.exception.plan;

import com.asaf.appointment_service.common.exception.BusinessException;

public class PlanLimitExceededException extends BusinessException {

    public PlanLimitExceededException(String message) {
        super("PLAN_LIMIT_EXCEEDED", message);
    }

    public PlanLimitExceededException(String code, String message) {
        super(code, message);
    }
}
