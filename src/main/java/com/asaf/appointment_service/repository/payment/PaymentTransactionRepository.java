package com.asaf.appointment_service.repository.payment;

import com.asaf.appointment_service.entity.payment.PaymentTransaction;
import com.asaf.appointment_service.entity.payment.PaymentTransactionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByMerchantOid(String merchantOid);

    List<PaymentTransaction> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    boolean existsByMerchantOidAndStatus(String merchantOid, PaymentTransactionStatus status);
}
