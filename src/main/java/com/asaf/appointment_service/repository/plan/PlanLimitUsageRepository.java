package com.asaf.appointment_service.repository.plan;

import com.asaf.appointment_service.entity.plan.PlanLimitUsage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanLimitUsageRepository extends JpaRepository<PlanLimitUsage, Long> {

    Optional<PlanLimitUsage> findByBusinessIdAndYearAndMonth(Long businessId, int year, int month);
}
