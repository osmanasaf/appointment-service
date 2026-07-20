package com.asaf.appointment_service.service.appointment;

import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.repository.appointment.AppointmentRepository;
import com.asaf.appointment_service.repository.employee.EmployeeLeaveRepository;
import com.asaf.appointment_service.service.employee.EmployeeCapabilityService;
import com.asaf.appointment_service.service.employee.EmployeeScheduleService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AppointmentValidationServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private EmployeeCapabilityService employeeCapabilityService;
    @Mock
    private EmployeeScheduleService employeeScheduleService;
    @Mock
    private EmployeeLeaveRepository employeeLeaveRepository;

    @InjectMocks
    private AppointmentValidationService validationService;

    private Business businessWithPolicy(boolean sameDayAllowed, int maxAdvanceDays) {
        return Business.builder()
            .name("Test").slug("test").category(BusinessCategory.OTHER)
            .sameDayBookingAllowed(sameDayAllowed).maxAdvanceBookingDays(maxAdvanceDays)
            .build();
    }

    private ServiceDefinition serviceWithSameDay(boolean sameDayAllowed) {
        return ServiceDefinition.builder()
            .businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0)
            .sameDayBookingAllowed(sameDayAllowed)
            .build();
    }

    @Test
    void should_throwValidationException_when_dateIsInThePast() {
        Business business = businessWithPolicy(true, 30);
        ServiceDefinition service = serviceWithSameDay(true);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> validationService.validateBookingPolicy(business, yesterday, service))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Geçmiş");
    }

    @Test
    void should_throwValidationException_when_sameDayBookingNotAllowedByBusiness() {
        Business business = businessWithPolicy(false, 30);
        ServiceDefinition service = serviceWithSameDay(true);

        assertThatThrownBy(() -> validationService.validateBookingPolicy(business, LocalDate.now(), service))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Aynı gün");
    }

    @Test
    void should_throwValidationException_when_sameDayBookingNotAllowedByService() {
        Business business = businessWithPolicy(true, 30);
        ServiceDefinition service = serviceWithSameDay(false);

        assertThatThrownBy(() -> validationService.validateBookingPolicy(business, LocalDate.now(), service))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("aynı gün");
    }

    @Test
    void should_throwValidationException_when_dateExceedsMaxAdvanceBookingDays() {
        Business business = businessWithPolicy(true, 10);
        ServiceDefinition service = serviceWithSameDay(true);
        LocalDate tooFar = LocalDate.now().plusDays(11);

        assertThatThrownBy(() -> validationService.validateBookingPolicy(business, tooFar, service))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("10 gün");
    }

    @Test
    void should_notThrow_when_dateWithinPolicy() {
        Business business = businessWithPolicy(true, 30);
        ServiceDefinition service = serviceWithSameDay(true);
        LocalDate validDate = LocalDate.now().plusDays(5);

        assertThatCode(() -> validationService.validateBookingPolicy(business, validDate, service))
            .doesNotThrowAnyException();
    }
}
