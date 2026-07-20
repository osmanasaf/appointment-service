package com.asaf.appointment_service.repository.waitlist;

import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.entity.waitlist.WaitlistStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByBusinessIdAndStatusOrderByCreatedAtAsc(Long businessId, WaitlistStatus status);

    List<WaitlistEntry> findByBusinessIdAndServiceIdAndStatusOrderByCreatedAtAsc(
        Long businessId, Long serviceId, WaitlistStatus status);

    List<WaitlistEntry> findByStatusAndNotifiedAtBefore(WaitlistStatus status, LocalDateTime cutoff);

    /**
     * Eşzamanlı SlotFreedEvent'lerin aynı kaydı çift teklif etmesini önlemek için
     * satırı kilitleyerek okur (WaitlistMatchingService.sendOffer öncesi kullanılır).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WaitlistEntry w WHERE w.id = :id")
    Optional<WaitlistEntry> findByIdForUpdate(@Param("id") Long id);
}
