package com.asaf.appointment_service.service.calendar;

import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.appointment.AppointmentStatus;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.service.ServiceDefinition;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.business.BusinessService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IcsServiceTest {

    @Mock
    private PublicActionTokenService tokenService;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private BusinessService businessService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;

    @InjectMocks
    private IcsService icsService;

    private Appointment appointmentWithStatus(AppointmentStatus status) {
        Appointment appointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(2L).serviceId(3L)
            .timeSlot(LocalDateTime.of(2026, 7, 22, 13, 0), 60)
            .status(status)
            .build();
        ReflectionTestUtils.setField(appointment, "id", 100L);
        return appointment;
    }

    private PublicActionToken tokenFor(Long appointmentId, String tokenValue) {
        return PublicActionToken.builder()
            .businessId(1L).subjectId(appointmentId).type(PublicActionTokenType.CALENDAR_ICS)
            .token(tokenValue).expiresAt(Instant.now().plusSeconds(3600)).build();
    }

    @Test
    void should_buildLinkWithIssuedToken_when_getOrIssueCalendarLinkCalled() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CONFIRMED);
        when(tokenService.getOrIssueCalendarIcsToken(1L, 100L)).thenReturn(tokenFor(100L, "tok123"));

        String link = icsService.getOrIssueCalendarLink("https://randevum.pro", appointment);

        assertThat(link).isEqualTo("https://randevum.pro/api/v1/public/appointments/tok123/calendar.ics");
    }

    @Test
    void should_generateNonCancelledIcs_when_appointmentIsConfirmed() {
        PublicActionToken token = tokenFor(100L, "tok123");
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CONFIRMED);
        Business business = Business.builder().id(1L).slug("kuafor-x").name("Kuaför X")
            .address("İstiklal Cad. No:1").category(com.asaf.appointment_service.entity.business.BusinessCategory.OTHER)
            .build();
        ServiceDefinition service = ServiceDefinition.builder()
            .id(3L).businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0).build();

        when(tokenService.validate("tok123", PublicActionTokenType.CALENDAR_ICS)).thenReturn(token);
        when(appointmentService.findByIdOrThrow(100L)).thenReturn(appointment);
        when(businessService.findByIdOrThrow(1L)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);

        String ics = icsService.generateIcsForToken("tok123");

        assertThat(ics).contains("METHOD:PUBLISH");
        assertThat(ics).contains("SUMMARY:Saç Kesimi - Kuaför X");
        assertThat(ics).contains("LOCATION:İstiklal Cad. No:1");
    }

    @Test
    void should_generateCancelledIcs_when_appointmentIsCancelled() {
        PublicActionToken token = tokenFor(100L, "tok123");
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CANCELLED);
        Business business = Business.builder().id(1L).slug("kuafor-x").name("Kuaför X")
            .category(com.asaf.appointment_service.entity.business.BusinessCategory.OTHER).build();
        ServiceDefinition service = ServiceDefinition.builder()
            .id(3L).businessId(1L).name("Saç Kesimi").durationMinutes(60).buffers(0, 0).build();

        when(tokenService.validate("tok123", PublicActionTokenType.CALENDAR_ICS)).thenReturn(token);
        when(appointmentService.findByIdOrThrow(100L)).thenReturn(appointment);
        when(businessService.findByIdOrThrow(1L)).thenReturn(business);
        when(serviceDefinitionService.findByIdOrThrow(3L)).thenReturn(service);

        String ics = icsService.generateIcsForToken("tok123");

        assertThat(ics).contains("METHOD:CANCEL");
        assertThat(ics).contains("STATUS:CANCELLED");
    }
}
