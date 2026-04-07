package com.asaf.appointment_service.service.plan;

import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.dto.appointment.StaffAppointmentRequest;
import com.asaf.appointment_service.dto.employee.CreateEmployeeRequest;
import com.asaf.appointment_service.dto.service.CreateServiceRequest;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PlanLimitAspect {

    private final SubscriptionService subscriptionService;

    public PlanLimitAspect(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Before("execution(* com.asaf.appointment_service.service.employee.EmployeeService.createEmployee(..)) && args(request)")
    public void checkEmployeeLimit(CreateEmployeeRequest request) {
        subscriptionService.checkEmployeeLimit(request.businessId());
    }

    @Before("execution(* com.asaf.appointment_service.service.service.ServiceDefinitionService.createService(..)) && args(request)")
    public void checkServiceLimit(CreateServiceRequest request) {
        subscriptionService.checkServiceLimit(request.businessId());
    }

    @Before("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createAppointment(..)) && args(request)")
    public void checkAppointmentLimit(CreateAppointmentRequest request) {
        subscriptionService.checkAppointmentLimit(request.businessId());
    }

    @Before("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createStaffAppointment(..)) && args(request)")
    public void checkStaffAppointmentLimit(StaffAppointmentRequest request) {
        subscriptionService.checkAppointmentLimit(request.businessId());
    }

    @AfterReturning("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createAppointment(..)) && args(request)")
    public void increaseAppointmentUsage(CreateAppointmentRequest request) {
        subscriptionService.incrementAppointmentUsage(request.businessId());
    }

    @AfterReturning("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createStaffAppointment(..)) && args(request)")
    public void increaseStaffAppointmentUsage(StaffAppointmentRequest request) {
        subscriptionService.incrementAppointmentUsage(request.businessId());
    }
}
