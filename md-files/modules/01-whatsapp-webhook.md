# Modül 01: WhatsApp Webhook İşleme

## Amaç
Gelen WhatsApp mesaj ve buton cevaplarını parse edip "Geliyorum" / "Onaylıyorum" → randevu onay, "İptal" / "İptal Ediyorum" → randevu iptal akışına bağlamak. Webhook doğrulama token'ı konfigürasyondan okunacak.

## Kabul kriterleri
- [ ] Webhook GET (verify): `hub.verify_token` config'den okunur; eşleşirse `hub.challenge` dönülür.
- [ ] Webhook POST: Payload'dan `entry[].changes[].value` içinde `messages` ve `statuses` parse edilir.
- [ ] Gelen mesajda metin veya buton cevabı "Geliyorum", "Onaylıyorum", "1" vb. ise ilgili randevu CONFIRMED yapılır.
- [ ] "İptal", "İptal Ediyorum", "2" vb. ise randevu CANCELLED yapılır (reason: "WhatsApp üzerinden iptal").
- [ ] Randevu eşleme: Gönderen telefon numarasına göre son 48 saatte gönderilmiş APPOINTMENT_REMINDER veya CONFIRMATION_REQUEST bildiriminden `appointmentId` alınır.
- [ ] Mesaj status güncellemeleri (delivered, read, failed) NotificationLog'da status güncellenebilir (opsiyonel ilk aşamada).

## Etkilenen / yeni dosyalar
- `src/main/java/com/asaf/appointment_service/controller/notification/WebhookController.java` — processWebhookPayload implementasyonu, verify token config
- `src/main/java/com/asaf/appointment_service/service/notification/WhatsAppWebhookService.java` (yeni) — payload parse, randevu eşleme, onay/iptal orchestration
- `src/main/java/com/asaf/appointment_service/repository/notification/NotificationLogRepository.java` — son reminder/confirmation log'u telefon ile bulma metodu
- `src/main/resources/application.properties` — `app.whatsapp.webhook.verify-token` (veya env)

## Teknik notlar
- WhatsApp Cloud API webhook payload: `object`, `entry[]` → `changes[]` → `value` içinde `messages` (from, text.body veya interactive.button_reply), `statuses` (id, status).
- Telefon eşleme: WhatsApp `from` alanı genelde ülke kodu ile (örn. 905551234567). NotificationLog.phoneNumber ile karşılaştırırken normalize edilmeli (başında + veya 0 olmadan).
- Randevu eşleme: Aynı numaraya birden fazla randevu hatırlatması gidebilir; en son gönderilen (createdAt desc) reminder/confirmation log'daki appointmentId kullanılır.
- State machine: Sadece PENDING veya RISKY randevu confirm edilebilir; sadece PENDING, CONFIRMED, RISKY iptal edilebilir.

## Alt görevler
- [ ] application.properties'e verify token key ekle
- [ ] NotificationLogRepository: findTopByPhoneNumberAndTypeInOrderByCreatedAtDesc (veya benzeri)
- [ ] WhatsAppWebhookService: parse payload, extract phone + message/button text
- [ ] WhatsAppWebhookService: resolveAppointmentId(phone), confirm/cancel çağrısı
- [ ] WebhookController: verify token'ı config'den oku; POST'ta WhatsAppWebhookService çağır
