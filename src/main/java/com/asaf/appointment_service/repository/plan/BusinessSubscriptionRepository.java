package com.asaf.appointment_service.repository.plan;

import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessSubscriptionRepository extends JpaRepository<BusinessSubscription, Long> {

    Optional<BusinessSubscription> findByBusinessId(Long businessId);
}
