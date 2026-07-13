package com.asaf.appointment_service.entity.payment;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 32)
    private String invoiceNumber;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    /**
     * PDF makbuz URL'si. Oluşturulunca set edilir; e-Arşiv fatura URL'si sonraki fazda eklenecek.
     */
    @Column(name = "pdf_url", length = 512)
    private String pdfUrl;

    protected Invoice() {}

    private Invoice(Builder builder) {
        this.businessId = builder.businessId;
        this.transactionId = builder.transactionId;
        this.invoiceNumber = builder.invoiceNumber;
        this.amount = builder.amount;
        this.taxAmount = builder.taxAmount;
        this.periodYear = builder.periodYear;
        this.periodMonth = builder.periodMonth;
        this.pdfUrl = builder.pdfUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public YearMonth getPeriod() {
        return YearMonth.of(periodYear, periodMonth);
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public int getPeriodYear() {
        return periodYear;
    }

    public int getPeriodMonth() {
        return periodMonth;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public static class Builder {
        private Long businessId;
        private Long transactionId;
        private String invoiceNumber;
        private BigDecimal amount;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private int periodYear;
        private int periodMonth;
        private String pdfUrl;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder taxAmount(BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        public Builder period(YearMonth period) {
            this.periodYear = period.getYear();
            this.periodMonth = period.getMonthValue();
            return this;
        }

        public Builder pdfUrl(String pdfUrl) {
            this.pdfUrl = pdfUrl;
            return this;
        }

        public Invoice build() {
            if (businessId == null || transactionId == null || invoiceNumber == null || amount == null) {
                throw new IllegalArgumentException("Invoice için zorunlu alanlar eksik");
            }
            if (periodYear <= 0 || periodMonth <= 0 || periodMonth > 12) {
                throw new IllegalArgumentException("Fatura dönemi geçersiz");
            }
            return new Invoice(this);
        }
    }
}
