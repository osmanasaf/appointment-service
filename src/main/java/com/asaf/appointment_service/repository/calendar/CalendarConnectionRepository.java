package com.asaf.appointment_service.repository.calendar;

import com.asaf.appointment_service.entity.calendar.CalendarConnection;
import com.asaf.appointment_service.entity.calendar.CalendarConnectionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarConnectionRepository extends JpaRepository<CalendarConnection, Long> {

    Optional<CalendarConnection> findByEmployeeId(Long employeeId);

    List<CalendarConnection> findByStatus(CalendarConnectionStatus status);

    void deleteByEmployeeId(Long employeeId);
}
