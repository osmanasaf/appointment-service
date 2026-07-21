package com.asaf.appointment_service.repository.calendar;

import com.asaf.appointment_service.entity.calendar.EmployeeExternalBusy;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeExternalBusyRepository extends JpaRepository<EmployeeExternalBusy, Long> {

    @Query("SELECT e FROM EmployeeExternalBusy e WHERE e.employeeId = :employeeId " +
           "AND e.startAt < :end AND e.endAt > :start")
    List<EmployeeExternalBusy> findOverlapping(
        @Param("employeeId") Long employeeId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);

    void deleteByEmployeeId(Long employeeId);
}
