package com.asaf.appointment_service.mapper.payment;

import com.asaf.appointment_service.dto.payment.InvoiceResponse;
import com.asaf.appointment_service.dto.payment.PaymentTransactionResponse;
import com.asaf.appointment_service.dto.payment.StoredCardResponse;
import com.asaf.appointment_service.entity.payment.Invoice;
import com.asaf.appointment_service.entity.payment.PaymentTransaction;
import com.asaf.appointment_service.entity.payment.StoredCard;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentTransactionResponse toResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getMerchantOid(),
                transaction.getPaymentAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getPaymentType(),
                transaction.getPlanCode(),
                transaction.getBillingPeriod(),
                transaction.getCreatedAt()
        );
    }

    public static StoredCardResponse toResponse(StoredCard card) {
        return new StoredCardResponse(
                card.getId(),
                card.getLast4(),
                card.getCardBrand(),
                card.getExpiryMonth(),
                card.getExpiryYear(),
                card.isDefault(),
                card.getCreatedAt()
        );
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getAmount(),
                invoice.getTaxAmount(),
                invoice.getPeriodYear(),
                invoice.getPeriodMonth(),
                invoice.getPdfUrl(),
                invoice.getCreatedAt()
        );
    }
}
