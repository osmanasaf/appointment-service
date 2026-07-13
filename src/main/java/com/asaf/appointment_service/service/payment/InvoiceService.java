package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.entity.payment.Invoice;
import com.asaf.appointment_service.entity.payment.PaymentTransaction;
import com.asaf.appointment_service.repository.payment.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.20");
    private static final String INVOICE_PREFIX = "INV";

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<Invoice> findByBusinessId(Long businessId) {
        return invoiceRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    @Transactional
    public Invoice createForTransaction(PaymentTransaction transaction) {
        if (invoiceRepository.findByTransactionId(transaction.getId()).isPresent()) {
            return invoiceRepository.findByTransactionId(transaction.getId()).orElseThrow();
        }

        String invoiceNumber = generateInvoiceNumber(transaction.getId());
        BigDecimal taxAmount = transaction.getPaymentAmount()
                .multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .businessId(transaction.getBusinessId())
                .transactionId(transaction.getId())
                .invoiceNumber(invoiceNumber)
                .amount(transaction.getPaymentAmount())
                .taxAmount(taxAmount)
                .period(YearMonth.now(ZoneOffset.UTC))
                .build();
        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber(Long transactionId) {
        return INVOICE_PREFIX + "-" + LocalDate.now(ZoneOffset.UTC).getYear()
                + "-" + String.format("%08d", transactionId);
    }
}
