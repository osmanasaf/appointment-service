package com.asaf.appointment_service.service.payment;

import com.asaf.appointment_service.common.exception.ErrorCode;
import com.asaf.appointment_service.common.util.MoneyUtil;
import com.asaf.appointment_service.entity.payment.BillingPeriod;
import com.asaf.appointment_service.entity.payment.Invoice;
import com.asaf.appointment_service.entity.payment.PaymentTransaction;
import com.asaf.appointment_service.entity.payment.PaymentType;
import com.asaf.appointment_service.entity.payment.StoredCard;
import com.asaf.appointment_service.entity.plan.BusinessSubscription;
import com.asaf.appointment_service.entity.plan.Plan;
import com.asaf.appointment_service.entity.plan.PlanCode;
import com.asaf.appointment_service.entity.plan.SubscriptionStatus;
import com.asaf.appointment_service.exception.payment.PaymentException;
import com.asaf.appointment_service.integration.paytr.PayTRClient;
import com.asaf.appointment_service.integration.paytr.PayTRHashService;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenRequest;
import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenResponse;
import com.asaf.appointment_service.repository.payment.PaymentTransactionRepository;
import com.asaf.appointment_service.repository.payment.StoredCardRepository;
import com.asaf.appointment_service.repository.plan.BusinessSubscriptionRepository;
import com.asaf.appointment_service.repository.plan.PlanRepository;
import com.asaf.appointment_service.service.encryption.EncryptionService;
import com.asaf.appointment_service.service.plan.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String CURRENCY_TL = "TL";

    private final PayTRClient payTRClient;
    private final PayTRHashService hashService;
    private final PayTRProperties payTRProperties;
    private final SubscriptionService subscriptionService;
    private final PaymentTransactionRepository transactionRepository;
    private final StoredCardRepository cardRepository;
    private final InvoiceService invoiceService;
    private final BusinessSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final EncryptionService encryptionService;

    public PaymentService(PayTRClient payTRClient,
                          PayTRHashService hashService,
                          PayTRProperties payTRProperties,
                          SubscriptionService subscriptionService,
                          PaymentTransactionRepository transactionRepository,
                          StoredCardRepository cardRepository,
                          InvoiceService invoiceService,
                          BusinessSubscriptionRepository subscriptionRepository,
                          PlanRepository planRepository,
                          EncryptionService encryptionService) {
        this.payTRClient = payTRClient;
        this.hashService = hashService;
        this.payTRProperties = payTRProperties;
        this.subscriptionService = subscriptionService;
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.invoiceService = invoiceService;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Plan için iFrame ödeme tokenı alır, PENDING transaction kaydı oluşturur.
     */
    @Transactional
    public IFramePaymentResult initiatePayment(Long businessId, PlanCode planCode,
                                               BillingPeriod billingPeriod,
                                               String userIp, String userEmail,
                                               String userName, String userPhone,
                                               String okUrl, String failUrl) {
        Plan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_INITIATION_FAILED,
                        "Plan bulunamadı: " + planCode));

        BigDecimal amount = resolveAmount(plan, billingPeriod);
        String merchantOid = generateMerchantOid(businessId);
        int amountKurus = MoneyUtil.toKurus(amount);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .businessId(businessId)
                .merchantOid(merchantOid)
                .paymentAmount(amount)
                .currency(CURRENCY_TL)
                .paymentType(resolvePaymentType(businessId))
                .planCode(planCode)
                .billingPeriod(billingPeriod)
                .build();
        transactionRepository.save(transaction);

        IFrameTokenRequest tokenRequest = new IFrameTokenRequest(
                merchantOid, userEmail, amountKurus, CURRENCY_TL,
                userIp, userName, userPhone, "-",
                okUrl, failUrl, true, 0,
                payTRProperties.testMode() ? 1 : 0
        );

        IFrameTokenResponse tokenResponse = payTRClient.getIFrameToken(tokenRequest);
        if (!tokenResponse.success()) {
            log.error("PayTR iFrame token alınamadı: businessId={}, reason={}",
                    businessId, tokenResponse.errorMessage());
            throw new PaymentException(ErrorCode.PAYMENT_INITIATION_FAILED, tokenResponse.errorMessage());
        }

        return new IFramePaymentResult(tokenResponse.token(), merchantOid, amount);
    }

    /**
     * PayTR webhook callback'ini işler. İdempotent — aynı merchantOid ikinci kez gelirse atlanır.
     */
    @Transactional
    public void processCallback(String merchantOid, String status, String totalAmount,
                                String hash, String utoken, String ctoken,
                                String last4, String cardBrand,
                                String expiryMonth, String expiryYear) {
        if (!hashService.verifyCallbackHash(merchantOid, status, totalAmount, hash)) {
            throw new PaymentException(ErrorCode.PAYMENT_HASH_INVALID);
        }

        PaymentTransaction transaction = transactionRepository.findByMerchantOid(merchantOid)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND,
                        "merchantOid: " + merchantOid));

        if (!transaction.isPending()) {
            log.info("Callback idempotent: merchantOid={} zaten işlenmiş ({})",
                    merchantOid, transaction.getStatus());
            return;
        }

        if ("success".equals(status)) {
            transaction.markSuccess();
            transactionRepository.save(transaction);
            activateSubscription(transaction, utoken, ctoken, last4, cardBrand,
                    expiryMonth, expiryYear);
            invoiceService.createForTransaction(transaction);
        } else {
            transaction.markFailed("PayTR callback status: " + status);
            transactionRepository.save(transaction);
            log.warn("Ödeme başarısız: merchantOid={}, status={}", merchantOid, status);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> getPaymentHistory(Long businessId) {
        return transactionRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    @Transactional(readOnly = true)
    public List<StoredCard> getStoredCards(Long businessId) {
        return cardRepository.findByBusinessIdOrderByIsDefaultDescCreatedAtDesc(businessId);
    }

    @Transactional
    public void deleteStoredCard(Long businessId, Long cardId) {
        StoredCard card = cardRepository.findById(cardId)
                .filter(c -> c.getBusinessId().equals(businessId))
                .orElseThrow(() -> new PaymentException(ErrorCode.CARD_NOT_FOUND));
        cardRepository.delete(card);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoices(Long businessId) {
        return invoiceService.findByBusinessId(businessId);
    }

    private void activateSubscription(PaymentTransaction transaction,
                                      String utoken, String ctoken,
                                      String last4, String cardBrand,
                                      String expiryMonth, String expiryYear) {
        Long businessId = transaction.getBusinessId();
        BillingPeriod period = transaction.getBillingPeriod();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Plan plan = planRepository.findByCode(transaction.getPlanCode())
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_INITIATION_FAILED));

        BusinessSubscription subscription = subscriptionService.getCurrentSubscriptionByBusinessId(businessId);

        LocalDate periodStart = resolveNewPeriodStart(subscription, plan.getCode(), today);
        long bonusDays = calculateUpgradeBonusDays(subscription, plan.getCode(), today);
        LocalDate periodEnd = resolvePeriodEnd(periodStart, period).plusDays(bonusDays);
        LocalDate nextPaymentDate = periodEnd.plusDays(1);

        String encryptedUtoken = utoken != null ? encryptionService.encrypt(utoken) : null;

        subscription.activatePayment(plan.getId(), period, periodStart, periodEnd, nextPaymentDate, encryptedUtoken);
        subscriptionRepository.save(subscription);

        if (ctoken != null && !ctoken.isBlank()) {
            saveStoredCard(businessId, ctoken, last4, cardBrand, expiryMonth, expiryYear);
        }
    }

    /**
     * Aynı plan hâlâ aktif bir dönemdeyken erken yenilenirse gün kaybı olmaması için
     * yeni dönem mevcut dönemin bitişinden başlar. Aksi halde (yeni abonelik, plan
     * değişikliği, süresi dolmuş dönem) bugünden başlar.
     */
    private LocalDate resolveNewPeriodStart(BusinessSubscription subscription, PlanCode newPlanCode, LocalDate today) {
        if (!isSamePlanActiveWithFuturePeriod(subscription, newPlanCode, today)) {
            return today;
        }
        return subscription.getCurrentPeriodEnd().plusDays(1);
    }

    /**
     * Plan yükseltmesi hâlâ aktif bir dönemin ortasında yapılırsa, eski plandan kalan
     * günler kullanıcı mağdur olmasın diye yeni dönemin sonuna bonus gün olarak eklenir.
     */
    private long calculateUpgradeBonusDays(BusinessSubscription subscription, PlanCode newPlanCode, LocalDate today) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                || subscription.getCurrentPeriodEnd() == null
                || !subscription.getCurrentPeriodEnd().isAfter(today)) {
            return 0;
        }
        Plan currentPlan = planRepository.findById(subscription.getPlanId()).orElse(null);
        if (currentPlan == null || currentPlan.getCode() == newPlanCode) {
            return 0;
        }
        return ChronoUnit.DAYS.between(today, subscription.getCurrentPeriodEnd());
    }

    private boolean isSamePlanActiveWithFuturePeriod(BusinessSubscription subscription, PlanCode newPlanCode, LocalDate today) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                || subscription.getCurrentPeriodEnd() == null
                || !subscription.getCurrentPeriodEnd().isAfter(today)) {
            return false;
        }
        Plan currentPlan = planRepository.findById(subscription.getPlanId()).orElse(null);
        return currentPlan != null && currentPlan.getCode() == newPlanCode;
    }

    private void saveStoredCard(Long businessId, String ctoken, String last4,
                                String cardBrand, String expiryMonth, String expiryYear) {
        String encryptedCtoken = encryptionService.encrypt(ctoken);
        cardRepository.findByBusinessIdAndIsDefaultTrue(businessId)
                .ifPresent(existing -> {
                    existing.removeDefault();
                    cardRepository.save(existing);
                });

        StoredCard card = StoredCard.builder()
                .businessId(businessId)
                .ctokenEncrypted(encryptedCtoken)
                .last4(last4 != null ? last4 : "****")
                .cardBrand(cardBrand)
                .expiryMonth(parseIntSafe(expiryMonth, 12))
                .expiryYear(parseIntSafe(expiryYear, 2099))
                .isDefault(true)
                .build();
        cardRepository.save(card);
    }

    private PaymentType resolvePaymentType(Long businessId) {
        return subscriptionRepository.findByBusinessId(businessId)
                .map(sub -> sub.isManual() ? PaymentType.NEW_SUBSCRIPTION : PaymentType.RENEWAL)
                .orElse(PaymentType.NEW_SUBSCRIPTION);
    }

    private BigDecimal resolveAmount(Plan plan, BillingPeriod billingPeriod) {
        return PaymentPricing.resolveAmount(plan, billingPeriod,
                payTRProperties.discount().quarterly(), payTRProperties.discount().semiannual());
    }

    private LocalDate resolvePeriodEnd(LocalDate start, BillingPeriod period) {
        return PaymentPricing.resolvePeriodEnd(start, period);
    }

    private String generateMerchantOid(Long businessId) {
        return "RNV-" + businessId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public record IFramePaymentResult(String iframeToken, String merchantOid, BigDecimal amount) {}
}
