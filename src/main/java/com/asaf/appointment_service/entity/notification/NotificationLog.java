package com.asaf.appointment_service.entity.notification;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
public class NotificationLog extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(length = 2000)
    private String message;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount = 0;

    protected NotificationLog() {}

    private NotificationLog(Builder builder) {
        this.businessId = builder.businessId;
        this.customerId = builder.customerId;
        this.appointmentId = builder.appointmentId;
        this.type = builder.type;
        this.phoneNumber = builder.phoneNumber;
        this.message = builder.message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markAsSent(String externalId) {
        this.status = NotificationStatus.SENT;
        this.externalId = externalId;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
    }

    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    public boolean canRetry() {
        return retryCount < 3 && status == NotificationStatus.FAILED;
    }

    public Long getBusinessId() { return businessId; }
    public Long getCustomerId() { return customerId; }
    public Long getAppointmentId() { return appointmentId; }
    public NotificationType getType() { return type; }
    public NotificationStatus getStatus() { return status; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public String getExternalId() { return externalId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }

    public static class Builder {
        private Long businessId;
        private Long customerId;
        private Long appointmentId;
        private NotificationType type;
        private String phoneNumber;
        private String message;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder appointmentId(Long appointmentId) {
            this.appointmentId = appointmentId;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public NotificationLog build() {
            if (businessId == null || type == null || phoneNumber == null) {
                throw new IllegalArgumentException("Business ID, type ve phone number zorunludur");
            }
            return new NotificationLog(this);
        }
    }
}
