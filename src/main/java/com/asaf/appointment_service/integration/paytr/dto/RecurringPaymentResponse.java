package com.asaf.appointment_service.integration.paytr.dto;

public record RecurringPaymentResponse(
    String status,
    boolean success,
    String errorMessage
) {

    public static RecurringPaymentResponse ofSuccess() {
        return new RecurringPaymentResponse("success", true, null);
    }

    public static RecurringPaymentResponse ofFailure(String errorMessage) {
        return new RecurringPaymentResponse("failed", false, errorMessage);
    }

    public boolean isPending() {
        return "wait_callback".equals(status);
    }
}
