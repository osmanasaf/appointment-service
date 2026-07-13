package com.asaf.appointment_service.controller.payment;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.payment.InitiatePaymentRequest;
import com.asaf.appointment_service.dto.payment.InitiatePaymentResponse;
import com.asaf.appointment_service.dto.payment.InvoiceResponse;
import com.asaf.appointment_service.dto.payment.PaymentTransactionResponse;
import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import com.asaf.appointment_service.mapper.payment.PaymentMapper;
import com.asaf.appointment_service.service.payment.PaymentService;
import com.asaf.appointment_service.service.payment.PaymentService.IFramePaymentResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final String IFRAME_BASE_URL = "https://www.paytr.com/odeme/guvenli/";

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;
    private final PayTRProperties payTRProperties;

    public PaymentController(PaymentService paymentService,
                             CurrentUserService currentUserService,
                             PayTRProperties payTRProperties) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
        this.payTRProperties = payTRProperties;
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            HttpServletRequest httpRequest) {
        currentUserService.requireBusinessOwnerOrAdmin();
        Long businessId = currentUserService.getCurrentBusinessId();
        String userIp = resolveClientIp(httpRequest);
        String userEmail = currentUserService.getCurrentUser().getEmail();
        String userName = userEmail;
        String userPhone = "-";

        IFramePaymentResult result = paymentService.initiatePayment(
                businessId, request.planCode(), request.billingPeriod(),
                userIp, userEmail, userName, userPhone,
                request.okUrl(), request.failUrl()
        );

        InitiatePaymentResponse response = new InitiatePaymentResponse(
                result.iframeToken(),
                result.merchantOid(),
                result.amount(),
                "TL",
                IFRAME_BASE_URL + result.iframeToken()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getPaymentHistory() {
        Long businessId = currentUserService.getCurrentBusinessId();
        List<PaymentTransactionResponse> history = paymentService.getPaymentHistory(businessId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoices() {
        Long businessId = currentUserService.getCurrentBusinessId();
        List<InvoiceResponse> invoices = paymentService.getInvoices(businessId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
