package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.business.BusinessCategory;
import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.payment.PaymentRetry;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.integration.paytr.PayTRClient;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentRequest;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentResponse;
import com.asaf.appointment_service.repository.business.BusinessRepository;
import com.asaf.appointment_service.repository.payment.PaymentRetryRepository;
import com.asaf.appointment_service.repository.payment.PaymentTransactionRepository;
import com.asaf.appointment_service.repository.payment.StoredCardRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringPaymentSchedulerTest {

    private static final Long BUSINESS_ID = 42L;
    private static final Long PLAN_ID = 7L;

    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private PayTRClient payTRClient;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private PaymentTransactionRepository transactionRepository;
    @Mock
    private PaymentRetryRepository retryRepository;
    @Mock
    private BusinessSubscriptionRepository subscriptionRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private StoredCardRepository storedCardRepository;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private BusinessRepository businessRepository;

    private final PayTRProperties payTRProperties = new PayTRProperties(
            "123456", "key", "salt", true, "https://www.paytr.com", true,
            new PayTRProperties.Discount(new BigDecimal("0.05"), new BigDecimal("0.10")));
    private final EmailProperties emailProperties =
            new EmailProperties("noreply@test.com", "https://app.randevum.pro", null);

    private RecurringPaymentScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RecurringPaymentScheduler(subscriptionService, payTRClient, payTRProperties,
                encryptionService, transactionRepository, retryRepository, subscriptionRepository,
                planRepository, storedCardRepository, invoiceService, businessRepository, emailProperties);
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");
    }

    @Test
    void should_useRealBusinessContactInfo_when_businessExists() {
        BusinessSubscription subscription = subscriptionWithBillingPeriod(BillingPeriod.MONTHLY);
        Plan plan = plan(PlanCode.PRO);
        Business business = Business.builder()
                .name("Ayşe Kuaför")
                .slug("ayse-kuafor")
                .phone("+90", "5551234567")
                .email("ayse@kuafor.com")
                .category(BusinessCategory.HAIR_SALON)
                .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));
        when(payTRClient.chargeRecurring(any())).thenReturn(RecurringPaymentResponse.ofSuccess());

        scheduler.processRenewal(subscription, LocalDate.now());

        ArgumentCaptor<RecurringPaymentRequest> captor = ArgumentCaptor.forClass(RecurringPaymentRequest.class);
        verify(payTRClient).chargeRecurring(captor.capture());
        RecurringPaymentRequest request = captor.getValue();

        assertThat(request.email()).isEqualTo("ayse@kuafor.com");
        assertThat(request.userName()).isEqualTo("Ayşe Kuaför");
        assertThat(request.userPhone()).isEqualTo("+905551234567");
        assertThat(request.merchantOkUrl()).isEqualTo("https://app.randevum.pro/admin/settings/billing/success");
        assertThat(request.merchantFailUrl()).isEqualTo("https://app.randevum.pro/admin/settings/billing/fail");
    }

    @Test
    void should_fallBackToSyntheticContactInfo_when_businessNotFound() {
        BusinessSubscription subscription = subscriptionWithBillingPeriod(BillingPeriod.MONTHLY);
        Plan plan = plan(PlanCode.PRO);

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.empty());
        when(payTRClient.chargeRecurring(any())).thenReturn(RecurringPaymentResponse.ofSuccess());

        scheduler.processRenewal(subscription, LocalDate.now());

        ArgumentCaptor<RecurringPaymentRequest> captor = ArgumentCaptor.forClass(RecurringPaymentRequest.class);
        verify(payTRClient).chargeRecurring(captor.capture());

        assertThat(captor.getValue().email()).isEqualTo(BUSINESS_ID + "@system.randevum.pro");
    }

    @Test
    void should_renewSubscriptionAndClearRetry_when_chargeSucceeds() {
        LocalDate today = LocalDate.now();
        BusinessSubscription subscription = subscriptionWithBillingPeriod(BillingPeriod.MONTHLY);
        Plan plan = plan(PlanCode.PRO);

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.empty());
        when(payTRClient.chargeRecurring(any())).thenReturn(RecurringPaymentResponse.ofSuccess());
        when(retryRepository.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        scheduler.processRenewal(subscription, today);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(today);
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(today.plusMonths(1).minusDays(1));
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void should_scheduleRetry_when_chargeFails() {
        LocalDate today = LocalDate.now();
        BusinessSubscription subscription = subscriptionWithBillingPeriod(BillingPeriod.MONTHLY);
        Plan plan = plan(PlanCode.PRO);

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.empty());
        when(payTRClient.chargeRecurring(any())).thenReturn(RecurringPaymentResponse.ofFailure("insufficient_funds"));
        when(retryRepository.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        scheduler.processRenewal(subscription, today);

        verify(retryRepository).save(any(PaymentRetry.class));
        verify(subscriptionRepository, never()).save(any());
    }

    private BusinessSubscription subscriptionWithBillingPeriod(BillingPeriod period) {
        LocalDate today = LocalDate.now();
        return BusinessSubscription.builder()
                .businessId(BUSINESS_ID)
                .planId(PLAN_ID)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(today.minusMonths(1))
                .currentPeriodEnd(today.minusDays(1))
                .manual(false)
                .billingPeriod(period)
                .paytrUtokenEncrypted("encrypted-utoken")
                .build();
    }

    private Plan plan(PlanCode code) {
        Plan plan = Plan.builder()
                .code(code)
                .maxEmployees(5)
                .maxAppointmentsMonthly(500)
                .maxServices(20)
                .maxBranches(1)
                .maxWhatsappMonthly(200)
                .maxSmsMonthly(100)
                .monthlyPrice(new BigDecimal("1000.00"))
                .yearlyPrice(new BigDecimal("11000.00"))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(plan, "id", PLAN_ID);
        return plan;
    }
}
