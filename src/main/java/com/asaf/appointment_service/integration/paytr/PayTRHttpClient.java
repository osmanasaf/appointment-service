package com.asaf.appointment_service.integration.paytr;

import com.asaf.appointment_service.integration.paytr.config.PayTRProperties;
import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenRequest;
import com.asaf.appointment_service.integration.paytr.dto.IFrameTokenResponse;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentRequest;
import com.asaf.appointment_service.integration.paytr.dto.RecurringPaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayTRHttpClient implements PayTRClient {

    private static final Logger log = LoggerFactory.getLogger(PayTRHttpClient.class);
    private static final String IFRAME_TOKEN_PATH = "/odeme/api/get-token";
    private static final String PAYMENT_PATH = "/odeme";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final int RECURRING_INSTALLMENT_COUNT = 0;
    private static final int RECURRING_NON_3D = 1;

    private final PayTRProperties properties;
    private final PayTRHashService hashService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PayTRHttpClient(PayTRProperties properties, PayTRHashService hashService,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public IFrameTokenResponse getIFrameToken(IFrameTokenRequest request) {
        int testMode = properties.testMode() ? 1 : 0;
        String paytrToken = hashService.computeIFrameHash(
                request.userIp(), request.merchantOid(), request.email(),
                request.paymentAmount(), "card", request.installmentCount(),
                request.currency(), testMode, 0
        );

        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", properties.merchantId());
        params.put("user_ip", request.userIp());
        params.put("merchant_oid", request.merchantOid());
        params.put("email", request.email());
        params.put("payment_amount", String.valueOf(request.paymentAmount()));
        params.put("paytr_token", paytrToken);
        params.put("payment_type", "card");
        params.put("installment_count", String.valueOf(request.installmentCount()));
        params.put("currency", request.currency());
        params.put("test_mode", String.valueOf(testMode));
        params.put("non_3d", "0");
        params.put("store_card", request.storeCard() ? "1" : "0");
        params.put("user_name", request.userName());
        params.put("user_phone", request.userPhone());
        params.put("user_address", request.userAddress());
        params.put("merchant_ok_url", request.merchantOkUrl());
        params.put("merchant_fail_url", request.merchantFailUrl());
        params.put("lang", "tr");
        params.put("debug_on", "0");

        try {
            String responseBody = post(IFRAME_TOKEN_PATH, params);
            JsonNode json = objectMapper.readTree(responseBody);
            String status = json.path("status").asText();
            if ("success".equals(status)) {
                return IFrameTokenResponse.success(json.path("token").asText());
            }
            String reason = json.path("reason").asText("Bilinmeyen hata");
            log.warn("PayTR iFrame token alınamadı: merchantOid={}, reason={}", request.merchantOid(), reason);
            return IFrameTokenResponse.failure(reason);
        } catch (Exception e) {
            log.error("PayTR iFrame token isteği başarısız: merchantOid={}", request.merchantOid(), e);
            return IFrameTokenResponse.failure("PayTR ile iletişim kurulamadı");
        }
    }

    @Override
    public RecurringPaymentResponse chargeRecurring(RecurringPaymentRequest request) {
        int testMode = properties.testMode() ? 1 : 0;
        String paytrToken = hashService.computeIFrameHash(
                request.userIp(), request.merchantOid(), request.email(),
                request.paymentAmount(), "card", RECURRING_INSTALLMENT_COUNT,
                request.currency(), testMode, RECURRING_NON_3D
        );

        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", properties.merchantId());
        params.put("user_ip", request.userIp());
        params.put("merchant_oid", request.merchantOid());
        params.put("email", request.email());
        params.put("payment_amount", String.valueOf(request.paymentAmount()));
        params.put("paytr_token", paytrToken);
        params.put("payment_type", "card");
        params.put("installment_count", String.valueOf(RECURRING_INSTALLMENT_COUNT));
        params.put("currency", request.currency());
        params.put("test_mode", String.valueOf(testMode));
        params.put("non_3d", String.valueOf(RECURRING_NON_3D));
        params.put("recurring_payment", "1");
        params.put("user_name", request.userName());
        params.put("user_address", request.userAddress());
        params.put("user_phone", request.userPhone());
        params.put("merchant_ok_url", request.merchantOkUrl());
        params.put("merchant_fail_url", request.merchantFailUrl());
        params.put("utoken", request.utoken());
        params.put("ctoken", request.ctoken());

        try {
            String responseBody = post(PAYMENT_PATH, params);
            JsonNode json = objectMapper.readTree(responseBody);
            String status = json.path("status").asText();
            return switch (status) {
                case "success" -> RecurringPaymentResponse.ofSuccess();
                case "wait_callback" -> new RecurringPaymentResponse("wait_callback", true, null);
                default -> {
                    String reason = json.path("err_no").asText(status);
                    log.warn("PayTR recurring başarısız: merchantOid={}, reason={}", request.merchantOid(), reason);
                    yield RecurringPaymentResponse.ofFailure(reason);
                }
            };
        } catch (Exception e) {
            log.error("PayTR recurring isteği başarısız: merchantOid={}", request.merchantOid(), e);
            return RecurringPaymentResponse.ofFailure("PayTR ile iletişim kurulamadı");
        }
    }

    private String post(String path, Map<String, String> params) throws IOException, InterruptedException {
        String formBody = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + path))
                .header("Content-Type", CONTENT_TYPE_FORM)
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
