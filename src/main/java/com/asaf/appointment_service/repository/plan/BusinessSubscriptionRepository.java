package com.asaf.appointment_service.repository.plan;

import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessSubscriptionRepository extends JpaRepository<BusinessSubscription, Long> {

    Optional<BusinessSubscription> findByBusinessId(Long businessId);

    long countByStatus(SubscriptionStatus status);

    List<BusinessSubscription> findByStatus(SubscriptionStatus status);

    long countByPlanIdAndStatus(Long planId, SubscriptionStatus status);
}
