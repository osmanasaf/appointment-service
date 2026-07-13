package com.asaf.appointment_service.repository.payment;

import com.asaf.appointment_service.entity.payment.PaymentRetry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRetryRepository extends JpaRepository<PaymentRetry, Long> {

    Optional<PaymentRetry> findByBusinessId(Long businessId);

    List<PaymentRetry> findByNextRetryDateLessThanEqual(LocalDate date);

    void deleteByBusinessId(Long businessId);
}
