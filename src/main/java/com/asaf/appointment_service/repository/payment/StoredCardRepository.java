package com.asaf.appointment_service.repository.payment;

import com.asaf.appointment_service.entity.payment.StoredCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredCardRepository extends JpaRepository<StoredCard, Long> {

    List<StoredCard> findByBusinessIdOrderByIsDefaultDescCreatedAtDesc(Long businessId);

    Optional<StoredCard> findByBusinessIdAndIsDefaultTrue(Long businessId);

    void deleteByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessId(Long businessId);
}
