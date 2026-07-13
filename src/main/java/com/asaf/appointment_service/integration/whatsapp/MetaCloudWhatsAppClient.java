package com.asaf.appointment_service.integration.whatsapp;

import com.asaf.appointment_service.integration.whatsapp.config.WhatsAppProperties;
import com.asaf.appointment_service.integration.whatsapp.dto.MetaMessageRequest;
import com.asaf.appointment_service.integration.whatsapp.dto.MetaMessageRequest.Component;
import com.asaf.appointment_service.integration.whatsapp.dto.MetaMessageRequest.Parameter;
import com.asaf.appointment_service.integration.whatsapp.dto.MetaMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetaCloudWhatsAppClient implements WhatsAppClientInterface {

    private static final Logger log = LoggerFactory.getLogger(MetaCloudWhatsAppClient.class);
    private static final String META_GRAPH_API_BASE = "https://graph.facebook.com";

    private final WhatsAppProperties properties;
    private final RestClient restClient;

    public MetaCloudWhatsAppClient(WhatsAppProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public WhatsAppMessageResponse sendMessage(WhatsAppMessageRequest request) {
        String to = normalizePhoneNumber(request.to());
        
        List<Parameter> bodyParams = new ArrayList<>();
        Map<String, String> params = request.parameters();
        
        if (params != null) {
            List<String> orderedKeys = getOrderedParameterKeys(request.templateName());
            for (String key : orderedKeys) {
                String value = params.get(key);
                if (value != null) {
                    bodyParams.add(Parameter.text(value));
                }
            }
        }

        List<Component> components = bodyParams.isEmpty() 
            ? null 
            : List.of(Component.body(bodyParams));

        MetaMessageRequest metaRequest = MetaMessageRequest.template(
            to,
            request.templateName(),
            "tr",
            components
        );

        return sendToMetaApi(metaRequest);
    }

    @Override
    public WhatsAppMessageResponse sendTextMessage(String to, String text) {
        String normalizedTo = normalizePhoneNumber(to);
        MetaMessageRequest metaRequest = MetaMessageRequest.text(normalizedTo, text, false);
        return sendToMetaApi(metaRequest);
    }

    @Override
    public WhatsAppMessageResponse sendOtp(String to, String code) {
        WhatsAppMessageRequest request = WhatsAppMessageRequest.otpVerification(to, code);
        return sendMessage(request);
    }

    @Override
    public WhatsAppMessageResponse sendInteractiveButtons(String to, String header, String body, String footer, List<ButtonOption> buttons) {
        String normalizedTo = normalizePhoneNumber(to);
        
        List<MetaMessageRequest.Button> metaButtons = buttons.stream()
            .map(b -> MetaMessageRequest.Button.reply(b.id(), b.title()))
            .toList();
        
        MetaMessageRequest metaRequest = MetaMessageRequest.interactiveButtons(
            normalizedTo, header, body, footer, metaButtons
        );
        
        return sendToMetaApi(metaRequest);
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    private WhatsAppMessageResponse sendToMetaApi(MetaMessageRequest request) {
        String url = buildApiUrl();
        
        try {
            MetaMessageResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.meta().accessToken())
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("Meta API hatası: status={}, body={}", res.getStatusCode(), 
                        new String(res.getBody().readAllBytes()));
                })
                .body(MetaMessageResponse.class);

            if (response == null) {
                log.error("Meta API boş response döndü");
                return WhatsAppMessageResponse.failure("Boş response");
            }

            if (response.isSuccess()) {
                String messageId = response.getFirstMessageId();
                log.info("WhatsApp mesajı gönderildi: to={}, messageId={}", 
                    maskPhone(request.to()), messageId);
                return WhatsAppMessageResponse.success(messageId);
            } else {
                String errorMsg = response.getErrorMessage();
                log.error("WhatsApp mesajı gönderilemedi: to={}, error={}", 
                    maskPhone(request.to()), errorMsg);
                return WhatsAppMessageResponse.failure(errorMsg);
            }
        } catch (Exception e) {
            log.error("WhatsApp API çağrısı başarısız: to={}, error={}", 
                maskPhone(request.to()), e.getMessage(), e);
            return WhatsAppMessageResponse.failure(e.getMessage());
        }
    }

    private String buildApiUrl() {
        String apiVersion = properties.meta().apiVersion();
        String phoneNumberId = properties.meta().phoneNumberId();
        return META_GRAPH_API_BASE + "/" + apiVersion + "/" + phoneNumberId + "/messages";
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0")) {
            digits = "90" + digits.substring(1);
        }
        if (!digits.startsWith("90") && digits.length() == 10) {
            digits = "90" + digits;
        }
        return digits;
    }

    private List<String> getOrderedParameterKeys(String templateName) {
        return switch (templateName) {
            case "appointment_reminder" -> List.of("customer_name", "date", "time", "service_name");
            case "appointment_confirmation" -> List.of("customer_name", "date", "time", "service_name");
            case "appointment_cancelled" -> List.of("customer_name", "date", "service_name");
            case "otp_verification" -> List.of("code");
            case "subscription_renewal_reminder" ->
                List.of("business_name", "plan_name", "days_before", "end_date", "renew_link");
            case "subscription_expired" -> List.of("business_name", "renew_link");
            default -> List.of("customer_name", "service_name", "date", "time", "price", "confirm_link");
        };
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
