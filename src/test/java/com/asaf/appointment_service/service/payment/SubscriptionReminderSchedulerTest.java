package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.integration.email.EmailClient;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionReminderSchedulerTest {

    private static final Long BUSINESS_ID = 7L;

    @Mock private BusinessSubscriptionRepository subscriptionRepository;
    @Mock private PlanRepository planRepository;
    @Mock private BusinessService businessService;
    @Mock private WhatsAppClientInterface whatsAppClient;
    @Mock private EmailClient emailClient;
    @Mock private EmailProperties emailProperties;

    private SubscriptionReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SubscriptionReminderScheduler(subscriptionRepository, planRepository,
                businessService, whatsAppClient, emailClient, emailProperties);
        ReflectionTestUtils.setField(scheduler, "gracePeriodDays", 3);
        lenient().when(emailProperties.baseUrl()).thenReturn("https://app.randevum.pro");
        lenient().when(whatsAppClient.isEnabled()).thenReturn(false);
    }

    @Test
    void should_sendReminderAndMarkSent_when_sevenDaysBeforePeriodEnd() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BusinessSubscription subscription = activeSubscription(today.plusDays(7));
        Business business = businessWithEmail("owner@test.com");

        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));
        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);

        scheduler.processSubscriptionReminders();

        verify(emailClient).sendSubscriptionRenewalReminder(
                org.mockito.ArgumentMatchers.eq("owner@test.com"), any(), any(), any(), org.mockito.ArgumentMatchers.eq(7), any());
        assertThat(subscription.getLastReminderDaysBefore()).isEqualTo(7);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void should_notSendDuplicateReminder_when_alreadySentForSameThreshold() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BusinessSubscription subscription = activeSubscription(today.plusDays(3));
        subscription.markReminderSent(3);

        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));

        scheduler.processSubscriptionReminders();

        verify(emailClient, never()).sendSubscriptionRenewalReminder(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
        verify(businessService, never()).findByIdOrThrow(any());
    }

    @Test
    void should_expireSubscription_when_gracePeriodPassed() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BusinessSubscription subscription = activeSubscription(today.minusDays(5));
        Business business = businessWithEmail("owner@test.com");

        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));
        when(businessService.findByIdOrThrow(BUSINESS_ID)).thenReturn(business);

        scheduler.processSubscriptionReminders();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(emailClient).sendSubscriptionExpired(org.mockito.ArgumentMatchers.eq("owner@test.com"), any(), any());
    }

    @Test
    void should_notExpire_when_withinGracePeriod() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BusinessSubscription subscription = activeSubscription(today.minusDays(2));

        when(subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));

        scheduler.processSubscriptionReminders();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(emailClient, never()).sendSubscriptionExpired(any(), any(), any());
    }

    private BusinessSubscription activeSubscription(LocalDate periodEnd) {
        return BusinessSubscription.builder()
                .businessId(BUSINESS_ID)
                .planId(1L)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(periodEnd.minusMonths(1))
                .currentPeriodEnd(periodEnd)
                .manual(false)
                .build();
    }

    private Business businessWithEmail(String email) {
        return Business.builder()
                .id(BUSINESS_ID)
                .name("Test İşletme")
                .email(email)
                .build();
    }

}
