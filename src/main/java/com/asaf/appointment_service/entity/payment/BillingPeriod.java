package com.asaf.appointment_service.entity.payment;

public enum BillingPeriod {
    MONTHLY(1),
    QUARTERLY(3),
    SEMIANNUAL(6),
    YEARLY(12);

    private final int months;

    BillingPeriod(int months) {
        this.months = months;
    }

    public int getMonths() {
        return months;
    }
}
