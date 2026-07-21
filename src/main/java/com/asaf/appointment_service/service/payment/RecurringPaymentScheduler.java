package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.common.util.MoneyUtil;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.payment.PaymentRetry;
import com.asaf.appointment_service.entity.payment.PaymentTransaction;
import com.asaf.appointment_service.entity.payment.PaymentTransactionStatus;
import com.asaf.appointment_service.entity.payment.PaymentType;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.integration.email.EmailProperties;
import com.asaf.appointment_service.integration.paytr.PayTRClient;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentRequest;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentResponse;
import com.asaf.appointment_service.repository.business.BusinessRepository;
import com.asaf.appointment_service.repository.payment.PaymentRetryRepository;
import com.asaf.appointment_service.repository.payment.StoredCardRepository;
import com.asaf.appointment_service.repository.payment.PaymentTransactionRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringPaymentScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringPaymentScheduler.class);
    private static final String SYSTEM_IP = "127.0.0.1";
    private static final String SYSTEM_EMAIL_SUFFIX = "@system.randevum.pro";
    private static final String FALLBACK_PHONE = "-";
    private static final String FALLBACK_ADDRESS = "-";
    private static final String BILLING_OK_PATH = "/admin/settings/billing/success";
    private static final String BILLING_FAIL_PATH = "/admin/settings/billing/fail";

    private final SubscriptionService subscriptionService;
    private final PayTRClient payTRClient;
    private final PayTRProperties payTRProperties;
    private final EncryptionService encryptionService;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentRetryRepository retryRepository;
    private final BusinessSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StoredCardRepository storedCardRepository;
    private final InvoiceService invoiceService;
    private final BusinessRepository businessRepository;
    private final EmailProperties emailProperties;

    public RecurringPaymentScheduler(SubscriptionService subscriptionService,
                                     PayTRClient payTRClient,
                                     PayTRProperties payTRProperties,
                                     EncryptionService encryptionService,
                                     PaymentTransactionRepository transactionRepository,
                                     PaymentRetryRepository retryRepository,
                                     BusinessSubscriptionRepository subscriptionRepository,
                                     PlanRepository planRepository,
                                     StoredCardRepository storedCardRepository,
                                     InvoiceService invoiceService,
                                     BusinessRepository businessRepository,
                                     EmailProperties emailProperties) {
        this.subscriptionService = subscriptionService;
        this.payTRClient = payTRClient;
        this.payTRProperties = payTRProperties;
        this.encryptionService = encryptionService;
        this.transactionRepository = transactionRepository;
        this.retryRepository = retryRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.storedCardRepository = storedCardRepository;
        this.invoiceService = invoiceService;
        this.businessRepository = businessRepository;
        this.emailProperties = emailProperties;
    }

    @Scheduled(cron = "${paytr.recurring.cron:0 0 3 * * *}", zone = "UTC")
    public void processRecurringPayments() {
        if (!payTRProperties.recurringEnabled()) {
            log.debug("Recurring scheduler devre dışı (paytr.recurring-enabled=false)");
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        log.info("Recurring ödeme scheduler çalışıyor: {}", today);

        List<BusinessSubscription> dueSubscriptions = subscriptionService.findSubscriptionsDueForRenewal(today);
        log.info("{} abonelik yenileme için sırada", dueSubscriptions.size());

        for (BusinessSubscription subscription : dueSubscriptions) {
            try {
                processRenewal(subscription, today);
            } catch (Exception e) {
                log.error("Recurring işlem hatası: businessId={}", subscription.getBusinessId(), e);
            }
        }
    }

    @Scheduled(cron = "${paytr.retry.cron:0 30 3 * * *}", zone = "UTC")
    public void processFailedRetries() {
        if (!payTRProperties.recurringEnabled()) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<PaymentRetry> retries = retryRepository.findByNextRetryDateLessThanEqual(today);
        log.info("{} başarısız ödeme retry için sırada", retries.size());

        for (PaymentRetry retry : retries) {
            try {
                processRetry(retry, today);
            } catch (Exception e) {
                log.error("Retry işlem hatası: businessId={}", retry.getBusinessId(), e);
            }
        }
    }

    @Transactional
    protected void processRenewal(BusinessSubscription subscription, LocalDate today) {
        Long businessId = subscription.getBusinessId();
        Plan plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new IllegalStateException("Plan bulunamadı: " + subscription.getPlanId()));

        RecurringPaymentResponse response = chargeSubscription(subscription, plan);

        if (response.success() && !response.isPending()) {
            onRenewalSuccess(subscription, plan, today);
        } else if (!response.success()) {
            onRenewalFailure(businessId, response.errorMessage(), today);
        }
    }

    @Transactional
    protected void processRetry(PaymentRetry retry, LocalDate today) {
        Long businessId = retry.getBusinessId();
        BusinessSubscription subscription = subscriptionRepository.findByBusinessId(businessId)
                .orElseThrow();
        Plan plan = planRepository.findById(subscription.getPlanId()).orElseThrow();

        RecurringPaymentResponse response = chargeSubscription(subscription, plan);

        if (response.success() && !response.isPending()) {
            retryRepository.deleteByBusinessId(businessId);
            onRenewalSuccess(subscription, plan, today);
        } else if (retry.hasMoreRetries()) {
            retry.scheduleNext(today);
            retry.recordError(response.errorMessage());
            retryRepository.save(retry);
            log.warn("Retry başarısız, sonraki deneme {}: businessId={}", retry.getNextRetryDate(), businessId);
        } else {
            retryRepository.delete(retry);
            subscription.suspend();
            subscriptionRepository.save(subscription);
            log.error("Maksimum retry aşıldı, abonelik askıya alındı: businessId={}", businessId);
        }
    }

    private RecurringPaymentResponse chargeSubscription(BusinessSubscription subscription, Plan plan) {
        Long businessId = subscription.getBusinessId();
        BillingPeriod period = subscription.getBillingPeriod() != null
                ? subscription.getBillingPeriod() : BillingPeriod.MONTHLY;
        BigDecimal amount = PaymentPricing.resolveAmount(plan, period,
                payTRProperties.discount().quarterly(), payTRProperties.discount().semiannual());

        String utoken = encryptionService.decrypt(subscription.getPaytrUtokenEncrypted());
        String ctoken = resolveDefaultCtoken(businessId);
        Business business = businessRepository.findById(businessId).orElse(null);

        String merchantOid = "RNV-" + businessId + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        RecurringPaymentRequest request = new RecurringPaymentRequest(
                merchantOid,
                resolveBusinessEmail(business, businessId),
                MoneyUtil.toKurus(amount),
                "TL",
                SYSTEM_IP,
                resolveBusinessName(business, businessId),
                FALLBACK_ADDRESS,
                resolveBusinessPhone(business),
                emailProperties.baseUrl() + BILLING_OK_PATH,
                emailProperties.baseUrl() + BILLING_FAIL_PATH,
                utoken,
                ctoken,
                payTRProperties.testMode() ? 1 : 0
        );

        PaymentTransaction transaction = PaymentTransaction.builder()
                .businessId(businessId)
                .merchantOid(merchantOid)
                .paymentAmount(amount)
                .currency("TL")
                .paymentType(PaymentType.RENEWAL)
                .planCode(plan.getCode())
                .billingPeriod(period)
                .build();
        transactionRepository.save(transaction);

        RecurringPaymentResponse response = payTRClient.chargeRecurring(request);

        if (response.success()) {
            transaction.markSuccess();
        } else {
            transaction.markFailed(response.errorMessage());
        }
        transactionRepository.save(transaction);

        return response;
    }

    private void onRenewalSuccess(BusinessSubscription subscription, Plan plan, LocalDate today) {
        BillingPeriod period = subscription.getBillingPeriod() != null
                ? subscription.getBillingPeriod() : BillingPeriod.MONTHLY;
        LocalDate periodEnd = PaymentPricing.resolvePeriodEnd(today, period);
        subscription.renewPayment(today, periodEnd, periodEnd.plusDays(1));
        subscriptionRepository.save(subscription);
        retryRepository.findByBusinessId(subscription.getBusinessId())
                .ifPresent(retryRepository::delete);
        log.info("Abonelik başarıyla yenilendi: businessId={}", subscription.getBusinessId());
    }

    private void onRenewalFailure(Long businessId, String error, LocalDate today) {
        if (retryRepository.findByBusinessId(businessId).isEmpty()) {
            PaymentRetry retry = PaymentRetry.create(businessId, today);
            retry.recordError(error);
            retryRepository.save(retry);
            log.warn("Ödeme başarısız, retry planlandı: businessId={}, nextRetry={}",
                    businessId, retry.getNextRetryDate());
        }
    }

    private String resolveDefaultCtoken(Long businessId) {
        return storedCardRepository.findByBusinessIdAndIsDefaultTrue(businessId)
                .map(card -> encryptionService.decrypt(card.getCtokenEncrypted()))
                .orElse(null);
    }

    private String resolveBusinessEmail(Business business, Long businessId) {
        if (business != null && business.getEmail() != null && !business.getEmail().isBlank()) {
            return business.getEmail();
        }
        return businessId + SYSTEM_EMAIL_SUFFIX;
    }

    private String resolveBusinessName(Business business, Long businessId) {
        if (business != null && business.getName() != null && !business.getName().isBlank()) {
            return business.getName();
        }
        return "Business " + businessId;
    }

    private String resolveBusinessPhone(Business business) {
        String phone = business != null ? business.getFormattedPhoneNumber() : null;
        return phone != null && !phone.isBlank() ? phone : FALLBACK_PHONE;
    }
}
