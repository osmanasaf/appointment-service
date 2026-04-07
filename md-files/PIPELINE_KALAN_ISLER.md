# Geliştirme Pipeline — Kalan İşler

**Tarih:** 5 Mart 2025  
**Kapsam:** Tüm kalan backend modülleri ve iyileştirmeler.

---

## 1. Gereksinimler (Yapılandırılmış)

| ID | Gereksinim | Kaynak | Öncelik |
|----|------------|--------|---------|
| R1 | WhatsApp başarısız olduğunda SMS ile fallback gönderimi | Modül 05 | Yüksek |
| R2 | SmsClient arayüzü ve konfigürasyon (sms.enabled, provider) | Modül 05 | Yüksek |
| R3 | Randevu create/confirm/cancel/complete sonrası domain event yayımlama | Modül 06 | Orta |
| R4 | Webhook POST için X-Hub-Signature-256 imza doğrulama | Güvenlik | Orta |
| R5 | Production CORS ve (opsiyonel) webhook rate limit | Güvenlik | Orta |
| R6 | PROJE_DURUM_RAPORU güncelleme | Dokümantasyon | Düşük |

---

## 2. Bağlam (Etkilenen Kod)

- **NotificationService:** `sendAndLog` — WhatsApp `response.success()` false ise SMS tetiklenebilir.
- **AppointmentService:** `createAppointment`, `confirmAppointment`, `cancelAppointment`, `completeAppointment` — event publish noktaları.
- **WebhookController:** POST `/whatsapp` — imza doğrulama eklenecek.
- **Paket yapısı:** `domain/valueobject` var; `domain/event` ve `integration/sms` eklenecek.

---

## 3. Kurallar (Özet)

- DRY, tek sorumluluk, 20+ satır metod parçalama (.cursor/rules/dry-cleancode.mdc).
- Proje exception hiyerarşisi, merkezi handler (.cursor/rules/error-handling.mdc).
- Hassas veri loglama yok, env ile config (.cursor/rules/security-performance.mdc).
- Test: methodName_givenCondition_expectedResult, AAA (md-files/testing.md).

---

## 4. Fazlı Plan

| Faz | İşler | Çıktı |
|-----|--------|--------|
| **Faz 1** | Domain Events (Modül 06) | Event sınıfları, AppointmentService publish |
| **Faz 2** | SMS Fallback (Modül 05) | SmsClient, MockSmsClient, NotificationService entegrasyonu, config |
| **Faz 3** | Güvenlik | Webhook imza (opsiyonel bu turda), CORS config |
| **Faz 4** | Dokümantasyon | PROJE_DURUM_RAPORU güncelleme |

---

## 5. Görev Listesi (Task)

| # | Görev | Dosya / Aksiyon |
|---|--------|------------------|
| 1 | AppointmentCreated event sınıfı | domain/event/AppointmentCreated.java |
| 2 | AppointmentConfirmed event sınıfı | domain/event/AppointmentConfirmed.java |
| 3 | AppointmentCancelled event sınıfı | domain/event/AppointmentCancelled.java |
| 4 | AppointmentCompleted event sınıfı | domain/event/AppointmentCompleted.java |
| 5 | AppointmentService'e ApplicationEventPublisher inject et, create/confirm/cancel/complete sonrası publish | AppointmentService.java |
| 6 | SmsClient interface | integration/sms/SmsClient.java |
| 7 | MockSmsClient (log only) | integration/sms/MockSmsClient.java |
| 8 | application.properties: sms.enabled, sms.from | application.properties |
| 9 | NotificationService: WhatsApp fail + sms.enabled ise SMS gönder | NotificationService.java |
| 10 | CORS allow origins (application.properties + WebMvcConfigurer) | WebMvcConfig veya application |
| 11 | PROJE_DURUM_RAPORU güncelle | PROJE_DURUM_RAPORU.md |

---

*Webhook imza (R4) ve rate limit (R5) bu turda opsiyonel bırakıldı; istenirse sonraki turda eklenir.*
