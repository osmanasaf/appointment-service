# Appointment Service — Proje Durum Raporu

**Tarih:** 5 Mart 2025 (güncellenme: pipeline tamamlandı)  
**Proje:** WhatsApp-First Randevu ve Takip Sistemi (Backend)

---

## 1. Proje Amacı ve Hedef

### Amaç
Sağlık ve güzellik profesyonellerinin (psikolog, diyetisyen, kuaför, güzellik merkezi vb.):
- **Gelmeyen randevuları (no-show)** azaltmak
- **Paket/seans takibini** otomatikleştirmek
- **WhatsApp** üzerinden randevu onayı ve hatırlatma ile operasyonel yükü azaltmak

### Temel Değer Önerisi
*“Gelmeyen randevuları azaltır, boş saatleri kurtarır.”*

### Ürün Kapsamı
- **B2B SaaS** (işletmenin kendi müşterisi)
- **WhatsApp-first** iletişim
- **Slot bazlı değil, işlem süresi bazlı** takvim (15 dk – 6 saat)
- Mobil uyumlu web (ilk etapta mobil app yok)

---

## 2. MVP Kapsamı (Hedef Liste)

| MVP Özelliği | Hedef | Durum |
|--------------|--------|-------|
| Online randevu linki | İşletmeye özel link (slug) | ✅ **Var** (`/api/v1/public/...`) |
| Takvim (günlük/haftalık) | Randevu listeleme, müsaitlik | ✅ **Backend hazır** (frontend yok) |
| İşlem bazlı süre (15 dk – 6 saat) | Hizmet süresi + buffer | ✅ **Var** (`ServiceDefinition`) |
| Süreye göre akıllı saat önerisi | Sadece gerçekten müsait saatler | ✅ **Var** (`AvailabilityService`) |
| Otomatik / manuel onaylı randevu | Anında onay / onay bekleyen | ✅ **Var** (slot etiketleri, `requiresConfirmation`) |
| Aynı gün randevu talebi | İzin / kural | ✅ **Var** (`sameDayBookingAllowed`) |
| Paket / seans takibi | Paket tanımı, seans düşümü | ✅ **Var** (`CustomerPackage`, `PackageService`) |
| WhatsApp randevu onayı (T-24) | 24 saat önce mesaj | ✅ **Var** (`ReminderScheduler`, `NotificationService`) |
| WhatsApp üzerinden iptal | Müşteri “İptal” deyince iptal | ⚠️ **Kısmi** (webhook altyapısı var, mesaj/buton işleme TODO) |
| Riskli randevu uyarısı | Onaylamayanlar riskli | ✅ **Var** (`AppointmentStatus.RISKY`, `markAsRisky`) |

---

## 3. Mevcut Teknik Durum

### 3.1 Tamamlanan Modüller

| Modül | Açıklama | Dosya/Özellik |
|-------|----------|----------------|
| **İşletme** | Profil, çalışma saatleri, slug | `Business`, `BusinessWorkingHours`, `BusinessService`, slug ile public erişim |
| **Çalışan** | Tanım, işletmeye bağlı | `Employee`, `EmployeeService`, `EmployeeController` |
| **Hizmet** | Süre, buffer, onay kuralı, paket uyumu | `ServiceDefinition` (duration, buffer, requiresConfirmation, packageEligible) |
| **Müşteri** | Ad, telefon | `Customer`, `CustomerService`, `CustomerController` |
| **Paket** | Seans sayısı, işlem ilişkisi | `CustomerPackage`, `PackageService`, `PackageController` |
| **Randevu** | CRUD, state machine | `Appointment`, `AppointmentStatus` (PENDING, CONFIRMED, RISKY, CANCELLED, COMPLETED, NO_SHOW) |
| **Müsaitlik** | İşlem süresi + çalışma saatleri + mevcut randevular | `AvailabilityService` — sadece gerçekten müsait slotlar, anında/onaylı etiket |
| **Public API** | Link üzerinden randevu | `PublicAppointmentController`: işletme bilgisi, hizmetler, çalışanlar, slotlar, randevu oluşturma |
| **Auth** | JWT, kayıt, giriş | `AuthController`, `JwtTokenProvider`, `SecurityConfig` |
| **Bildirim** | WhatsApp hatırlatma, onay isteği, iptal bildirimi | `NotificationService`, `WhatsAppClient`, `ReminderScheduler` (T-24) |
| **Webhook** | WhatsApp doğrulama ve POST endpoint | `WebhookController` — verify tamam; gelen mesaj/buton işleme **TODO** |
| **Domain** | Value object’ler | `TimeSlot`, `PhoneNumber`, `ServiceDuration`, `Money`, `WorkingHours` |

### 3.2 Teknoloji Özeti

- **Stack:** Spring Boot 4.0.1, Java 21, Spring Data JPA, Spring Security (JWT), H2 (geliştirme)
- **Mimari:** Katmanlı (entity, dto, repository, service, controller, mapper, exception, security, integration)
- **Veritabanı:** Geliştirme **H2** (in-memory). Production **PostgreSQL** (`application-prod.properties`, `ddl-auto=validate`).
- **CORS:** `app.cors.allowed-origins` ile yapılandırılabilir (`WebConfig`).

---

## 4. Eksik veya Kısmi Kalanlar

| Konu | Durum | Öneri |
|------|--------|--------|
| **Frontend** | Yok | React/Vue ile mobil uyumlu web planlandı, henüz başlanmamış (Modül 07) |
| **Webhook imza doğrulama** | POST’ta X-Hub-Signature-256 yok | HMAC doğrulama eklenebilir (güvenlik iyileştirmesi) |
| **Rate limiting** | Webhook/public endpoint’lerde yok | İsteğe bağlı (prod için) |
| **Gerçek SMS provider** | Sadece MockSmsClient | Twilio/Netgsm vb. impl eklenebilir (`app.sms.enabled=true` ile) |

---

## 5. İlerleme Özeti

| Kategori | Tahmini İlerleme | Açıklama |
|----------|-------------------|----------|
| **Backend MVP (API)** | **~95%** | CRUD, müsaitlik, public randevu, auth, bildirim, webhook, paket seans, fiyat/toggle |
| **WhatsApp entegrasyonu** | **~95%** | Gönderim + gelen mesaj (Geliyorum/İptal), fiyat bilgisi, toggle |
| **Paket / seans** | **~95%** | Randevu tamamlandığında otomatik seans düşümü (completeAppointment) |
| **Frontend** | **0%** | Başlanmamış |
| **Production hazırlık** | **~75%** | PostgreSQL profile, CORS config, unit testler; rate limit / webhook imza opsiyonel |

**Genel MVP backend ilerlemesi: yaklaşık %90.**  
Hedef: “İşlem süresi bazlı, WhatsApp-first, no-show azaltan randevu SaaS” — çekirdek mantık ve API büyük ölçüde mevcut; en kritik eksik WhatsApp’tan gelen onay/iptal cevabının işlenmesi ve (isteğe bağlı) PostgreSQL + testler.

---

## 6. Önerilen Sıradaki Adımlar

1. **Frontend:** React/Vue ile işletme paneli ve müşteri randevu sayfası (online randevu linki).
2. **Webhook imza:** POST'ta X-Hub-Signature-256 ile HMAC doğrulama (prod güvenliği).
3. **SMS provider:** Twilio/Netgsm impl (şu an MockSmsClient; app.sms.enabled=true ile gerçek gönderim).

---

*Bu rapor `md-files/project.md` ve mevcut kod tabanına göre oluşturulmuştur.*
