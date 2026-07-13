package com.asaf.appointment_service.entity.payment;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "payment_retries")
public class PaymentRetry extends BaseEntity {

    private static final int[] RETRY_DAYS = {1, 3, 7};

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_date", nullable = false)
    private LocalDate nextRetryDate;

    @Column(name = "last_error", length = 512)
    private String lastError;

    protected PaymentRetry() {}

    public static PaymentRetry create(Long businessId, LocalDate today) {
        PaymentRetry retry = new PaymentRetry();
        retry.businessId = businessId;
        retry.attemptCount = 1;
        retry.nextRetryDate = today.plusDays(RETRY_DAYS[0]);
        return retry;
    }

    public boolean hasMoreRetries() {
        return attemptCount < RETRY_DAYS.length;
    }

    public void scheduleNext(LocalDate today) {
        if (!hasMoreRetries()) {
            throw new IllegalStateException("Maksimum retry sayısına ulaşıldı");
        }
        this.nextRetryDate = today.plusDays(RETRY_DAYS[attemptCount]);
        this.attemptCount++;
    }

    public void recordError(String error) {
        this.lastError = error;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDate getNextRetryDate() {
        return nextRetryDate;
    }

    public String getLastError() {
        return lastError;
    }
}
