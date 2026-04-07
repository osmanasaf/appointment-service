package com.asaf.appointment_service.repository.business;

import com.asaf.appointment_service.entity.business.BusinessWorkingHours;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessWorkingHoursRepository extends JpaRepository<BusinessWorkingHours, Long> {

    List<BusinessWorkingHours> findByBusinessIdOrderByDayOfWeek(Long businessId);

    Optional<BusinessWorkingHours> findByBusinessIdAndDayOfWeek(Long businessId, DayOfWeek dayOfWeek);

    void deleteByBusinessId(Long businessId);
}
