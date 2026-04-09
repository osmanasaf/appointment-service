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

    @Before("execution(* com.asaf.appointment_service.service.employee.EmployeeService.createEmployee(..)) && args(businessId, request)")
    public void checkEmployeeLimit(Long businessId, CreateEmployeeRequest request) {
        subscriptionService.checkEmployeeLimit(businessId);
    }

    @Before("execution(* com.asaf.appointment_service.service.service.ServiceDefinitionService.createService(..)) && args(businessId, request)")
    public void checkServiceLimit(Long businessId, CreateServiceRequest request) {
        subscriptionService.checkServiceLimit(businessId);
    }

    @Before("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createAppointment(..)) && args(businessId, request)")
    public void checkAppointmentLimit(Long businessId, CreateAppointmentRequest request) {
        subscriptionService.checkAppointmentLimit(businessId);
    }

    @Before("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createStaffAppointment(..)) && args(businessId, request)")
    public void checkStaffAppointmentLimit(Long businessId, StaffAppointmentRequest request) {
        subscriptionService.checkAppointmentLimit(businessId);
    }

    @AfterReturning("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createAppointment(..)) && args(businessId, request)")
    public void increaseAppointmentUsage(Long businessId, CreateAppointmentRequest request) {
        subscriptionService.incrementAppointmentUsage(businessId);
    }

    @AfterReturning("execution(* com.asaf.appointment_service.service.appointment.AppointmentService.createStaffAppointment(..)) && args(businessId, request)")
    public void increaseStaffAppointmentUsage(Long businessId, StaffAppointmentRequest request) {
        subscriptionService.incrementAppointmentUsage(businessId);
    }
}
