# Modül 05: SMS Fallback (Opsiyonel)

## Amaç
WhatsApp gönderilemediğinde (hata veya kullanıcı WhatsApp kullanmıyorsa) SMS ile hatırlatma/onay gönderimi. Proje dokümanında fallback SMS geçmektedir.

## Kabul kriterleri
- [x] SMS provider entegrasyonu (Twilio, Netgsm veya benzeri) için bir SmsClient interface/impl veya adapter tanımlanır (integration/sms veya integration/sms altında).
- [x] NotificationService içinde: WhatsApp gönderimi başarısız olduğunda (veya konfigürasyona göre) SMS ile aynı içerik gönderilebilir.
- [x] Konfigürasyon: SMS aktif/pasif, provider bilgileri (API key, from number vb.) application.properties veya env ile verilir.

## Etkilenen / yeni dosyalar
- `src/main/java/com/asaf/appointment_service/integration/sms/SmsClient.java` (interface veya class)
- `src/main/java/com/asaf/appointment_service/integration/sms/...` (provider impl, opsiyonel)
- `src/main/java/com/asaf/appointment_service/service/notification/NotificationService.java` — WhatsApp fail sonrası SMS tetikleme
- `src/main/resources/application.properties` — sms.enabled, sms.provider, sms.api-key vb.

## Teknik notlar
- package-structure.md'de integration/sms/SmsClient referansı var.
- Bu modül opsiyonel; Faz 2 zorunlu sırada (1–4) yer almaz. İstenirse 4'ten sonra yapılır.

## Alt görevler
- [ ] SmsClient interface ve (örnek) mock veya Twilio/Netgsm impl
- [ ] NotificationService: WhatsApp catch bloğunda veya retry sonrası SMS gönderim seçeneği
- [ ] Konfigürasyon özellikleri
