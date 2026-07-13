package com.asaf.appointment_service.repository.payment;

import com.asaf.appointment_service.entity.payment.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<Invoice> findByTransactionId(Long transactionId);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
