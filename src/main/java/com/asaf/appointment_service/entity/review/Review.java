package com.asaf.appointment_service.entity.review;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Tamamlanan bir randevu için müşterinin bıraktığı değerlendirme.
 * Varsayılan olarak moderasyonludur (PENDING_APPROVAL); işletme onayı olmadan
 * public sayfada görünmez.
 */
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING_APPROVAL;

    protected Review() {}

    private Review(Builder builder) {
        if (builder.rating < 1 || builder.rating > 5) {
            throw new IllegalArgumentException("Puan 1-5 arasında olmalıdır");
        }
        this.businessId = builder.businessId;
        this.appointmentId = builder.appointmentId;
        this.customerId = builder.customerId;
        this.employeeId = builder.employeeId;
        this.rating = builder.rating;
        this.comment = builder.comment;
        this.status = ReviewStatus.PENDING_APPROVAL;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void publish() {
        this.status = ReviewStatus.PUBLISHED;
    }

    public void reject() {
        this.status = ReviewStatus.REJECTED;
    }

    public boolean isFiveStars() {
        return rating == 5;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public static class Builder {
        private Long businessId;
        private Long appointmentId;
        private Long customerId;
        private Long employeeId;
        private int rating;
        private String comment;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder appointmentId(Long appointmentId) {
            this.appointmentId = appointmentId;
            return this;
        }

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder rating(int rating) {
            this.rating = rating;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Review build() {
            if (businessId == null || appointmentId == null || customerId == null) {
                throw new IllegalArgumentException("İşletme, randevu ve müşteri ID zorunludur");
            }
            return new Review(this);
        }
    }
}
