package com.asaf.appointment_service.exception.plan;

import com.asaf.appointment_service.common.exception.BusinessException;

public class PlanFeatureNotAvailableException extends BusinessException {

    public PlanFeatureNotAvailableException(String featureCode) {
        super("FEATURE_NOT_AVAILABLE",
            "Bu özellik mevcut planınızda yok: " + featureCode + ". Planınızı yükseltin.");
    }
}
