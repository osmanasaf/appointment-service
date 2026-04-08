package com.asaf.appointment_service.repository.employee;

import com.asaf.appointment_service.entity.employee.EmployeeWorkingSchedule;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeWorkingScheduleRepository extends JpaRepository<EmployeeWorkingSchedule, Long> {

    List<EmployeeWorkingSchedule> findByEmployeeId(Long employeeId);

    Optional<EmployeeWorkingSchedule> findByEmployeeIdAndDayOfWeek(Long employeeId, DayOfWeek dayOfWeek);

    void deleteByEmployeeId(Long employeeId);
}
