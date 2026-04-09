package com.asaf.appointment_service.repository.customer;

import com.asaf.appointment_service.entity.customer.PackageSession;
import com.asaf.appointment_service.entity.customer.PackageSessionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageSessionRepository extends JpaRepository<PackageSession, Long> {

    List<PackageSession> findByCustomerPackageIdOrderBySessionNumberAsc(Long customerPackageId);

    Optional<PackageSession> findByAppointmentId(Long appointmentId);

    Optional<PackageSession> findFirstByCustomerPackageIdAndStatusOrderBySessionNumberAsc(
            Long customerPackageId, PackageSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM PackageSession ps WHERE ps.id = :id")
    Optional<PackageSession> findByIdWithLock(@Param("id") Long id);
}
