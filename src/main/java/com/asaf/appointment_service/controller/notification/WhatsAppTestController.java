package com.asaf.appointment_service.controller.notification;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppClientInterface.ButtonOption;
import com.asaf.appointment_service.integration.whatsapp.WhatsAppMessageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test/whatsapp")
@Profile("local")
public class WhatsAppTestController {

    private final WhatsAppClientInterface whatsAppClient;

    public WhatsAppTestController(WhatsAppClientInterface whatsAppClient) {
        this.whatsAppClient = whatsAppClient;
    }

    @PostMapping("/send-text")
    public ResponseEntity<ApiResponse<WhatsAppMessageResponse>> sendText(
            @RequestParam String to,
            @RequestParam String message) {
        WhatsAppMessageResponse response = whatsAppClient.sendTextMessage(to, message);
        return ResponseEntity.ok(ApiResponse.success(response, "Mesaj gönderildi"));
    }

    @PostMapping("/send-reminder")
    public ResponseEntity<ApiResponse<WhatsAppMessageResponse>> sendReminder(
            @RequestParam String to,
            @RequestParam String customerName,
            @RequestParam String serviceName,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam(defaultValue = "Randevum Pro") String businessName) {
        
        String header = "📅 Randevu Hatırlatma";
        String body = String.format("""
            Merhaba %s,
            
            %s işletmesinde %s tarihinde saat %s'de %s randevunuz bulunmaktadır.
            
            Lütfen katılım durumunuzu belirtin.""",
            customerName, businessName, date, time, serviceName);
        String footer = businessName;
        
        List<ButtonOption> buttons = List.of(
            new ButtonOption("confirm_appointment", "✅ Geliyorum"),
            new ButtonOption("cancel_appointment", "❌ İptal Et"),
            new ButtonOption("reschedule_appointment", "🔄 Ertele")
        );
        
        WhatsAppMessageResponse response = whatsAppClient.sendInteractiveButtons(to, header, body, footer, buttons);
        return ResponseEntity.ok(ApiResponse.success(response, "Hatırlatma gönderildi"));
    }

    @PostMapping("/send-confirmation")
    public ResponseEntity<ApiResponse<WhatsAppMessageResponse>> sendConfirmation(
            @RequestParam String to,
            @RequestParam String customerName,
            @RequestParam String serviceName,
            @RequestParam String date,
            @RequestParam String time) {
        
        String body = String.format("""
            ✅ Randevunuz Onaylandı!
            
            Merhaba %s,
            
            %s tarihinde saat %s'de %s randevunuz onaylanmıştır.
            
            Görüşmek üzere!""",
            customerName, date, time, serviceName);
        
        WhatsAppMessageResponse response = whatsAppClient.sendTextMessage(to, body);
        return ResponseEntity.ok(ApiResponse.success(response, "Onay mesajı gönderildi"));
    }
}