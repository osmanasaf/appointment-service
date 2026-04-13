package com.asaf.appointment_service.repository.employee;

import com.asaf.appointment_service.entity.employee.EmployeeLeave;
import com.asaf.appointment_service.entity.employee.LeaveStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long> {

    List<EmployeeLeave> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    @Query("SELECT l FROM EmployeeLeave l WHERE l.employeeId = :employeeId " +
           "AND l.status = 'ACTIVE' " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    List<EmployeeLeave> findActiveInDateRange(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT l FROM EmployeeLeave l WHERE l.employeeId = :employeeId " +
           "AND l.status = 'ACTIVE' " +
           "AND l.startDate <= :date AND l.endDate >= :date")
    List<EmployeeLeave> findActiveByEmployeeIdAndDate(
        @Param("employeeId") Long employeeId,
        @Param("date") LocalDate date
    );
}
