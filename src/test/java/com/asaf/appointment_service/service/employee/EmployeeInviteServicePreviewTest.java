package com.asaf.appointment_service.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.asaf.appointment_service.common.exception.ValidationException;
import com.asaf.appointment_service.dto.employee.EmployeeInvitePreviewResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeInvite;
import com.asaf.appointment_service.integration.email.EmailClient;
import com.asaf.appointment_service.repository.employee.EmployeeInviteRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeInviteServicePreviewTest {

    private static final Long BUSINESS_ID = 10L;
    private static final Long EMPLOYEE_ID = 20L;
    private static final String TOKEN = "abc123token";

    @Mock
    private EmployeeInviteRepository inviteRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private BusinessService businessService;
    @Mock
    private EmailClient emailClient;

    @InjectMocks
    private EmployeeInviteService inviteService;

    @Test
    void getPreviewByToken_should_returnEmailNameAndBusiness_when_inviteValid() {
        EmployeeInvite invite = validInvite();
        Employee employee = Employee.builder()
            .id(EMPLOYEE_ID)
            .businessId(BUSINESS_ID)
            .name("Jane Doe")
            .build();
        Business business = Business.builder()
            .id(BUSINESS_ID)
            .name("Acme Salon")
            .build();

        when(inviteRepository.findByToken(TOKEN)).thenReturn(Optional.of(invite));
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);
        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);

        EmployeeInvitePreviewResponse preview = inviteService.getPreviewByToken(TOKEN);

        assertThat(preview.email()).isEqualTo("invited@example.com");
        assertThat(preview.employeeName()).isEqualTo("Jane Doe");
        assertThat(preview.businessName()).isEqualTo("Acme Salon");
    }

    @Test
    void getPreviewByToken_should_throw_when_tokenBlank() {
        assertThatThrownBy(() -> inviteService.getPreviewByToken("  "))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Davet kodu");
    }

    @Test
    void getPreviewByToken_should_throw_when_inviteUnknown() {
        when(inviteRepository.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.getPreviewByToken(TOKEN))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("geçersiz");
    }

    @Test
    void getPreviewByToken_should_throw_when_inviteExpired() {
        EmployeeInvite invite = EmployeeInvite.builder()
            .businessId(BUSINESS_ID)
            .employeeId(EMPLOYEE_ID)
            .token(TOKEN)
            .email("invited@example.com")
            .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();

        when(inviteRepository.findByToken(TOKEN)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.getPreviewByToken(TOKEN))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("geçersiz");
    }

    @Test
    void assertRegistrationNameMatchesInvite_should_pass_when_namesMatchIgnoringCaseAndSpaces() {
        EmployeeInvite invite = validInvite();
        Employee employee = Employee.builder()
            .id(EMPLOYEE_ID)
            .businessId(BUSINESS_ID)
            .name("Jane  Doe")
            .build();
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);

        inviteService.assertRegistrationNameMatchesInvite(invite, "jane doe");
    }

    @Test
    void assertRegistrationNameMatchesInvite_should_throw_when_nameDiffers() {
        EmployeeInvite invite = validInvite();
        Employee employee = Employee.builder()
            .id(EMPLOYEE_ID)
            .businessId(BUSINESS_ID)
            .name("Jane Doe")
            .build();
        when(employeeService.findByIdOrThrow(EMPLOYEE_ID)).thenReturn(employee);

        assertThatThrownBy(() -> inviteService.assertRegistrationNameMatchesInvite(invite, "Other Person"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("eşleşmiyor");
    }

    private static EmployeeInvite validInvite() {
        return EmployeeInvite.builder()
            .businessId(BUSINESS_ID)
            .employeeId(EMPLOYEE_ID)
            .token(TOKEN)
            .email("invited@example.com")
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .build();
    }
}
