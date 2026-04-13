package com.asaf.appointment_service.repository.employee;

import com.asaf.appointment_service.entity.employee.EmployeeInvite;
import com.asaf.appointment_service.entity.employee.InviteStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeInviteRepository extends JpaRepository<EmployeeInvite, Long> {

    Optional<EmployeeInvite> findByToken(String token);

    List<EmployeeInvite> findByBusinessIdAndStatus(Long businessId, InviteStatus status);

    List<EmployeeInvite> findByEmployeeIdAndStatus(Long employeeId, InviteStatus status);
}
