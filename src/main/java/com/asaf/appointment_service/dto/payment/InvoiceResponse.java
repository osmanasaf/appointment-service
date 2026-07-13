package com.asaf.appointment_service.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
    Long id,
    String invoiceNumber,
    BigDecimal amount,
    BigDecimal taxAmount,
    int periodYear,
    int periodMonth,
    String pdfUrl,
    LocalDateTime createdAt
) {}
