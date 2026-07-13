package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.payment.PaymentTransaction;
import com.asaf.appointment_service.entity.payment.PaymentTransactionStatus;
import com.asaf.appointment_service.entity.payment.PaymentType;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.exception.payment.PaymentException;
import com.asaf.appointment_service.integration.paytr.PayTRClient;
import com.asaf.appointment_service.integration.paytr.PayTRHashService;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import com.asaf.appointment_service.repository.payment.PaymentTransactionRepository;
import com.asaf.appointment_service.repository.payment.StoredCardRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long BUSINESS_ID = 42L;
    private static final Long OLD_PLAN_ID = 1L;
    private static final Long NEW_PLAN_ID = 2L;
    private static final String MERCHANT_OID = "RNV-42-ABC123";

    @Mock
    private PayTRClient payTRClient;
    @Mock
    private PayTRHashService hashService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private PaymentTransactionRepository transactionRepository;
    @Mock
    private StoredCardRepository cardRepository;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private BusinessSubscriptionRepository subscriptionRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private EncryptionService encryptionService;

    private final PayTRProperties payTRProperties = new PayTRProperties(
            "123456", "key", "salt", true, "https://www.paytr.com", false,
            new PayTRProperties.Discount(new BigDecimal("0.05"), new BigDecimal("0.10")));

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(payTRClient, hashService, payTRProperties,
                subscriptionService, transactionRepository, cardRepository, invoiceService,
                subscriptionRepository, planRepository, encryptionService);
    }

    @Test
    void should_throwPaymentException_when_callbackHashInvalid() {
        when(hashService.verifyCallbackHash(MERCHANT_OID, "success", "100.00", "bad-hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> paymentService.processCallback(
                MERCHANT_OID, "success", "100.00", "bad-hash",
                null, null, null, null, null, null))
                .isInstanceOf(PaymentException.class);

        verify(transactionRepository, never()).findByMerchantOid(any());
    }

    @Test
    void should_skipProcessing_when_callbackAlreadyProcessed() {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .businessId(BUSINESS_ID)
                .merchantOid(MERCHANT_OID)
                .paymentAmount(new BigDecimal("100.00"))
                .paymentType(PaymentType.NEW_SUBSCRIPTION)
                .planCode(PlanCode.PRO)
                .billingPeriod(BillingPeriod.MONTHLY)
                .build();
        transaction.markSuccess();

        when(hashService.verifyCallbackHash(MERCHANT_OID, "success", "100.00", "valid-hash"))
                .thenReturn(true);
        when(transactionRepository.findByMerchantOid(MERCHANT_OID)).thenReturn(Optional.of(transaction));

        paymentService.processCallback(MERCHANT_OID, "success", "100.00", "valid-hash",
                null, null, null, null, null, null);

        verify(subscriptionService, never()).getCurrentSubscriptionByBusinessId(any());
        verify(invoiceService, never()).createForTransaction(any());
    }

    @Test
    void should_extendFromCurrentPeriodEnd_when_earlyRenewalOfSamePlan() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate oldPeriodEnd = today.plusDays(10);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .businessId(BUSINESS_ID)
                .merchantOid(MERCHANT_OID)
                .paymentAmount(new BigDecimal("1000.00"))
                .paymentType(PaymentType.RENEWAL)
                .planCode(PlanCode.PRO)
                .billingPeriod(BillingPeriod.MONTHLY)
                .build();

        Plan proPlan = createPlan(PlanCode.PRO, NEW_PLAN_ID);

        BusinessSubscription existingSubscription = BusinessSubscription.builder()
                .businessId(BUSINESS_ID)
                .planId(NEW_PLAN_ID)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(today.minusDays(20))
                .currentPeriodEnd(oldPeriodEnd)
                .manual(false)
                .build();

        when(hashService.verifyCallbackHash(MERCHANT_OID, "success", "1000.00", "valid-hash"))
                .thenReturn(true);
        when(transactionRepository.findByMerchantOid(MERCHANT_OID)).thenReturn(Optional.of(transaction));
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(proPlan));
        when(planRepository.findById(NEW_PLAN_ID)).thenReturn(Optional.of(proPlan));
        when(subscriptionService.getCurrentSubscriptionByBusinessId(BUSINESS_ID))
                .thenReturn(existingSubscription);

        paymentService.processCallback(MERCHANT_OID, "success", "1000.00", "valid-hash",
                null, null, null, null, null, null);

        assertThat(existingSubscription.getCurrentPeriodStart()).isEqualTo(oldPeriodEnd.plusDays(1));
        assertThat(existingSubscription.getCurrentPeriodEnd())
                .isEqualTo(oldPeriodEnd.plusDays(1).plusMonths(1).minusDays(1));
    }

    @Test
    void should_addRemainingDaysAsBonus_when_upgradingPlanMidPeriod() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate oldPeriodEnd = today.plusDays(10);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .businessId(BUSINESS_ID)
                .merchantOid(MERCHANT_OID)
                .paymentAmount(new BigDecimal("2000.00"))
                .paymentType(PaymentType.RENEWAL)
                .planCode(PlanCode.ENTERPRISE)
                .billingPeriod(BillingPeriod.MONTHLY)
                .build();

        Plan oldPlan = createPlan(PlanCode.PRO, OLD_PLAN_ID);
        Plan newPlan = createPlan(PlanCode.ENTERPRISE, NEW_PLAN_ID);

        BusinessSubscription existingSubscription = BusinessSubscription.builder()
                .businessId(BUSINESS_ID)
                .planId(OLD_PLAN_ID)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(today.minusDays(20))
                .currentPeriodEnd(oldPeriodEnd)
                .manual(false)
                .build();

        when(hashService.verifyCallbackHash(MERCHANT_OID, "success", "2000.00", "valid-hash"))
                .thenReturn(true);
        when(transactionRepository.findByMerchantOid(MERCHANT_OID)).thenReturn(Optional.of(transaction));
        when(planRepository.findByCode(PlanCode.ENTERPRISE)).thenReturn(Optional.of(newPlan));
        when(planRepository.findById(OLD_PLAN_ID)).thenReturn(Optional.of(oldPlan));
        when(subscriptionService.getCurrentSubscriptionByBusinessId(BUSINESS_ID))
                .thenReturn(existingSubscription);

        paymentService.processCallback(MERCHANT_OID, "success", "2000.00", "valid-hash",
                null, null, null, null, null, null);

        // Yeni dönem bugünden başlar + eski dönemden kalan 10 gün bonus olarak eklenir
        assertThat(existingSubscription.getCurrentPeriodStart()).isEqualTo(today);
        LocalDate expectedEnd = today.plusMonths(1).minusDays(1).plusDays(10);
        assertThat(existingSubscription.getCurrentPeriodEnd()).isEqualTo(expectedEnd);
    }

    private Plan createPlan(PlanCode code, Long id) {
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
        org.springframework.test.util.ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }
}
