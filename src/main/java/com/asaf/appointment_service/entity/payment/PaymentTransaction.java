package com.asaf.appointment_service.entity.payment;

import com.asaf.appointment_service.common.entity.BaseEntity;
import com.asaf.appointment_service.entity.plan.PlanCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "merchant_oid", nullable = false, unique = true, length = 64)
    private String merchantOid;

    @Column(name = "payment_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentTransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false)
    private PlanCode planCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Column(name = "paytr_token", length = 512)
    private String paytrToken;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    protected PaymentTransaction() {}

    private PaymentTransaction(Builder builder) {
        this.businessId = builder.businessId;
        this.merchantOid = builder.merchantOid;
        this.paymentAmount = builder.paymentAmount;
        this.currency = builder.currency;
        this.status = builder.status;
        this.paymentType = builder.paymentType;
        this.planCode = builder.planCode;
        this.billingPeriod = builder.billingPeriod;
        this.paytrToken = builder.paytrToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void markSuccess() {
        this.status = PaymentTransactionStatus.SUCCESS;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = PaymentTransactionStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public boolean isPending() {
        return status == PaymentTransactionStatus.PENDING;
    }

    public boolean isSuccess() {
        return status == PaymentTransactionStatus.SUCCESS;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getMerchantOid() {
        return merchantOid;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentTransactionStatus getStatus() {
        return status;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public PlanCode getPlanCode() {
        return planCode;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public String getPaytrToken() {
        return paytrToken;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static class Builder {
        private Long businessId;
        private String merchantOid;
        private BigDecimal paymentAmount;
        private String currency = "TL";
        private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;
        private PaymentType paymentType;
        private PlanCode planCode;
        private BillingPeriod billingPeriod = BillingPeriod.MONTHLY;
        private String paytrToken;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder merchantOid(String merchantOid) {
            this.merchantOid = merchantOid;
            return this;
        }

        public Builder paymentAmount(BigDecimal paymentAmount) {
            this.paymentAmount = paymentAmount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder status(PaymentTransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder paymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public Builder planCode(PlanCode planCode) {
            this.planCode = planCode;
            return this;
        }

        public Builder billingPeriod(BillingPeriod billingPeriod) {
            this.billingPeriod = billingPeriod;
            return this;
        }

        public Builder paytrToken(String paytrToken) {
            this.paytrToken = paytrToken;
            return this;
        }

        public PaymentTransaction build() {
            if (businessId == null || merchantOid == null || paymentAmount == null
                    || paymentType == null || planCode == null) {
                throw new IllegalArgumentException("PaymentTransaction için zorunlu alanlar eksik");
            }
            return new PaymentTransaction(this);
        }
    }
}
