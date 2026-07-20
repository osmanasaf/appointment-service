package com.asaf.appointment_service.service.waitlist;

import com.asaf.appointment_service.common.exception.BusinessException;
import com.asaf.appointment_service.dto.appointment.CreateAppointmentRequest;
import com.asaf.appointment_service.entity.appointment.Appointment;
import com.asaf.appointment_service.entity.customer.Customer;
import com.asaf.appointment_service.entity.plan.PlanFeature;
import com.asaf.appointment_service.entity.token.PublicActionToken;
import com.asaf.appointment_service.entity.token.PublicActionTokenType;
import com.asaf.appointment_service.entity.waitlist.WaitlistEntry;
import com.asaf.appointment_service.exception.plan.PlanFeatureNotAvailableException;
import com.asaf.appointment_service.exception.waitlist.WaitlistEntryNotFoundException;
import com.asaf.appointment_service.repository.waitlist.WaitlistEntryRepository;
import com.asaf.appointment_service.service.appointment.AppointmentService;
import com.asaf.appointment_service.service.appointment.AppointmentValidationService;
import com.asaf.appointment_service.service.customer.CustomerService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import com.asaf.appointment_service.service.service.ServiceDefinitionService;
import com.asaf.appointment_service.service.token.PublicActionTokenService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistEntryRepository repository;
    @Mock
    private CustomerService customerService;
    @Mock
    private ServiceDefinitionService serviceDefinitionService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private AppointmentValidationService validationService;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private PublicActionTokenService tokenService;

    @InjectMocks
    private WaitlistService waitlistService;

    @Test
    void should_createEntryForFoundOrCreatedCustomer_when_registerPublicCalled() {
        Customer customer = Customer.builder().businessId(1L).name("Ayşe").phone("+90", "5551112233").build();
        ReflectionTestUtils.setField(customer, "id", 10L);
        when(customerService.findOrCreateByPhone(1L, "Ayşe", "+90", "5551112233")).thenReturn(customer);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WaitlistEntry entry = waitlistService.registerPublic(1L, 3L, null, null, null, "Ayşe", "+90", "5551112233");

        verify(subscriptionService).requireFeature(1L, PlanFeature.WAITLIST);
        assertThat(entry.getBusinessId()).isEqualTo(1L);
        assertThat(entry.getCustomerId()).isEqualTo(10L);
        assertThat(entry.getServiceId()).isEqualTo(3L);
    }

    @Test
    void should_throwPlanFeatureNotAvailable_when_registerPublicCalledWithoutFeature() {
        doThrow(new PlanFeatureNotAvailableException(PlanFeature.WAITLIST))
            .when(subscriptionService).requireFeature(1L, PlanFeature.WAITLIST);

        assertThatThrownBy(() ->
            waitlistService.registerPublic(1L, 3L, null, null, null, "Ayşe", "+90", "5551112233"))
            .isInstanceOf(PlanFeatureNotAvailableException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void should_validateCapability_when_employeeIdProvidedOnManualAdd() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        waitlistService.addManually(1L, 10L, 3L, 5L, null, null);

        verify(validationService).validateCapability(5L, 3L);
    }

    @Test
    void should_cancelEntry_when_entryBelongsToBusiness() {
        WaitlistEntry entry = WaitlistEntry.builder().businessId(1L).customerId(10L).serviceId(3L).build();
        when(repository.findById(50L)).thenReturn(Optional.of(entry));

        waitlistService.cancelEntry(1L, 50L);

        assertThat(entry.getStatus().name()).isEqualTo("CANCELLED");
        verify(repository).save(entry);
    }

    @Test
    void should_throwWaitlistEntryNotFound_when_entryBelongsToDifferentBusiness() {
        WaitlistEntry entry = WaitlistEntry.builder().businessId(2L).customerId(10L).serviceId(3L).build();
        when(repository.findById(50L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> waitlistService.cancelEntry(1L, 50L))
            .isInstanceOf(WaitlistEntryNotFoundException.class);
    }

    @Test
    void should_createAppointmentAndConvertEntry_when_acceptOfferCalledOnNotifiedEntry() {
        WaitlistEntry entry = notifiedEntry();
        ReflectionTestUtils.setField(entry, "id", 99L);
        PublicActionToken token = tokenFor(99L);

        when(tokenService.validate("tok", PublicActionTokenType.WAITLIST_OFFER)).thenReturn(token);
        when(repository.findById(99L)).thenReturn(Optional.of(entry));
        Appointment createdAppointment = Appointment.builder()
            .businessId(1L).customerId(10L).employeeId(7L).serviceId(3L)
            .timeSlot(LocalDateTime.now().plusDays(1), 30)
            .build();
        when(appointmentService.createAppointment(eq(1L), any(CreateAppointmentRequest.class)))
            .thenReturn(createdAppointment);

        Appointment result = waitlistService.acceptOffer("tok");

        assertThat(result).isSameAs(createdAppointment);
        assertThat(entry.getStatus().name()).isEqualTo("CONVERTED");
        verify(tokenService).consume(token);
        verify(repository).save(entry);
    }

    @Test
    void should_throwBusinessException_when_acceptOfferCalledOnNonNotifiedEntry() {
        WaitlistEntry entry = WaitlistEntry.builder().businessId(1L).customerId(10L).serviceId(3L).build();
        ReflectionTestUtils.setField(entry, "id", 99L);
        PublicActionToken token = tokenFor(99L);

        when(tokenService.validate("tok", PublicActionTokenType.WAITLIST_OFFER)).thenReturn(token);
        when(repository.findById(99L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> waitlistService.acceptOffer("tok"))
            .isInstanceOf(BusinessException.class);

        verify(appointmentService, never()).createAppointment(any(), any());
        verify(tokenService, never()).consume(any());
    }

    private WaitlistEntry notifiedEntry() {
        WaitlistEntry entry = WaitlistEntry.builder().businessId(1L).customerId(10L).serviceId(3L).build();
        entry.markNotified(7L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));
        return entry;
    }

    private PublicActionToken tokenFor(Long subjectId) {
        return PublicActionToken.builder()
            .businessId(1L).subjectId(subjectId).type(PublicActionTokenType.WAITLIST_OFFER)
            .token("tok").expiresAt(Instant.now().plusSeconds(3600)).build();
    }
}
