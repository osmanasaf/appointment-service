# Development Context — Farklı Modellerle Geliştirme İçin Tek Dosya

> Bu dosyayı herhangi bir LLM'e (Claude, GPT, Gemini, Cursor, Cline vs.) açılışta ver. Projeyi anlatır, kuralları koyar, geliştirme için hazır context sağlar.

---

## 1. Proje Kimliği

**Ad:** appointment-service (kod adı)
**Ürün konumu:** Türkiye için B2B SaaS — küçük ve orta ölçekli hizmet işletmelerine (kuaför, güzellik, psikolog, diyetisyen, klinik) yönelik **WhatsApp-first randevu yönetim sistemi**.

**Ana farklılaştırıcılar:**
1. Gelmeyen randevu (no-show) azaltma: T-24 otomatik onay isteme + RISKY status + ileride depozito
2. Süre+buffer bazlı akıllı müsaitlik (slot bazlı değil; 15 dk – 6 sa)
3. Paket/seans entegre randevu (otomatik seans düşümü)
4. WhatsApp iki yönlü onay/iptal/erteleme
5. DDD mimari, value object'ler

**Rakipler:** planla.co, kolayrandevu (SalonAppy), randuapp, asistline, salonrandevu, randevu.cc

---

## 2. Teknik Stack

### Backend
- **Spring Boot 4.0.1**, **Java 21**
- Spring Data JPA, Spring Security (JWT)
- **H2** (dev), **PostgreSQL** (prod, `application-prod.properties`, `ddl-auto=validate`)
- Flyway (önerilen, henüz tam değil)
- Maven (pom.xml)
- Paket yapısı: `com.asaf.appointment_service`
  - `controller/*` (REST)
  - `service/*` (business logic)
  - `repository/*`
  - `entity/*`
  - `dto/*` + `mapper/*`
  - `domain/valueobject/*`, `domain/event/*`
  - `common/*` (config, exception, security, util)
  - `integration/whatsapp/*`

### Frontend
- **Vue 3** (Composition API), **Vite**, **TypeScript**
- **Pinia** (state)
- **vue-router**
- **axios**
- **Hedef ek bağımlılıklar (henüz kurulmamış):** Tailwind CSS 4, PrimeVue 4, Lucide Vue, vee-validate + zod, vue-sonner, ApexCharts Vue, FullCalendar Vue, vue-i18n
- Klasör: `frontend/src/{api,views,stores,router,components}`

### Mobil (planlanan)
- **Flutter** (önerilen) — alternatif React Native + Expo
- Firebase Cloud Messaging (push)

### Entegrasyonlar
- **WhatsApp Cloud API** (Meta) — gönderim ✓, webhook iki yönlü kısmi
- **iyzico** (Faz 7'de) — abonelik + 3DS
- **Nilvera** (Faz 6'da) — e-fatura
- **FCM** (Faz 7'de) — mobil push
- **SMS:** MockSmsClient mevcut → Netgsm/Twilio impl gelecek

---

## 3. Mevcut Backend Modülleri (özet)

| Modül | Durum | Not |
|---|---|---|
| Auth (JWT) | ✅ | Register/Login/Refresh |
| Business | ✅ | Slug, çalışma saatleri |
| Employee | ✅ | Temel CRUD — capability eklenecek |
| ServiceDefinition | ✅ | Süre, buffer, requiresConfirmation, packageEligible |
| Customer | ✅ | Telefon (PhoneNumber VO) |
| Package | ✅ | CustomerPackage, otomatik seans düşümü |
| Appointment | ✅ | State machine (PENDING/CONFIRMED/RISKY/COMPLETED/CANCELLED/NO_SHOW) |
| AvailabilityService | ✅ | Süre + buffer + çalışma saatleri |
| Public booking | ✅ | `/api/v1/public/{slug}/...` |
| WhatsApp Notification | ✅ | NotificationService + ReminderScheduler T-24 |
| Webhook | ⚠️ | Verify ✓, mesaj/buton işleme TODO + HMAC eksik |

---

## 4. Domain Sözlüğü

- **Business**: işletme (tenant)
- **Employee**: işletmenin çalışanı
- **Service / ServiceDefinition**: sunulan hizmet (saç kesim vb.)
- **Customer**: işletmenin müşterisi
- **Appointment**: randevu kaydı
- **Package**: müşteriye satılan paket (örn. 10 seans masaj)
- **Slot**: müsaitlik içindeki saat dilimi
- **Buffer**: hizmet sonu hazırlık/temizlik süresi
- **RISKY**: T-24'te onaylanmayan randevu
- **No-show**: müşterinin gelmediği randevu

---

## 5. Kod Standartları

- **Java:** Lombok kullanılıyor, Records DTO'larda tercih edilir
- **API yanıt formatı:** `ApiResponse<T>` (data, message, success, errors)
- **Hata yönetimi:** `GlobalExceptionHandler` + custom exception sınıfları
- **Validation:** Bean Validation + service katmanında business rule
- **Multi-tenant:** her sorgu `business_id` filtreli olmalı; `@CurrentBusiness` annotation yapılabilir
- **Test:** JUnit 5, Mockito, MockMvc; integration için Testcontainers (Postgres)
- **Naming:** İngilizce kod, Türkçe kullanıcı mesajları
- **Commit:** Conventional Commits (feat:, fix:, refactor:, docs:, test:, chore:)
- **Branch:** `feature/<modul>-<kisa-aciklama>`, `fix/...`

### Frontend
- **Composition API + `<script setup>`**
- **TypeScript strict mode**
- **API client:** axios instance + interceptor (auth header, 401 redirect)
- **State:** Pinia (`stores/`)
- **Form:** vee-validate + zod schema
- **Component naming:** PascalCase, view'lar `*View.vue`
- **CSS:** Tailwind utility-first, ortak component'ler `components/ui/`

---

## 6. Aktif Yol Haritası (Faz Sıralaması)

| # | Faz | Süre | Durum |
|---|---|---|---|
| 1 | Çalışan-hizmet yetkinliği | 2 hf | bekliyor |
| 2 | İşletme profili + sosyal medya + galeri | 1 hf | bekliyor |
| 3 | Modern public booking | 2-3 hf | bekliyor |
| 4 | Plan modeli (DB, ödeme yok) | 1 hf | bekliyor |
| 5 | Dashboard + frontend modernizasyon (PrimeVue/Tailwind) | 3-4 hf | bekliyor |
| 6 | E-Fatura (Premium plan, Nilvera) | 2 hf | bekliyor |
| 7 | Mobil (Flutter) + Abonelik/Ödeme (iyzico) BİRLİKTE | 8-10 hf | bekliyor |

---

## 7. Plan / Fiyatlandırma

| Plan | Çalışan | Aylık Randevu | Hizmet | Şube | E-Fatura | Fiyat |
|---|---|---|---|---|---|---|
| Başlangıç (Solo) | 1 | 150 | 10 | 1 | ❌ | 499 TL/ay |
| Profesyonel | 5 | 750 | 30 | 1 | ❌ | 999 TL/ay |
| İşletme | 15 | 2.500 | sınırsız | 3 | ❌ | 1.799 TL/ay |
| Premium | sınırsız | sınırsız | sınırsız | sınırsız | ✅ | 2.999 TL/ay |

14 gün ücretsiz deneme, yıllık %20 indirim.

---

## 8. Geliştiriciden Beklenen Davranış (LLM için kurallar)

Bu bölüm, projeye yeni bir LLM girdiğinde uyması gereken kuralları içerir:

1. **Önce mevcut kodu oku.** Yeni dosya yazmadan önce ilgili paketin yapısını incele. Aynı pattern'i takip et.
2. **Multi-tenant unutma.** Her yeni endpoint/sorgu `business_id` filtresi içermeli.
3. **DTO ve mapper kullan.** Entity'i controller'dan dönme.
4. **Service katmanında business rule.** Controller ince, service kalın.
5. **Validation ikili:** Bean Validation (form) + service katmanı (kural).
6. **Migration yaz.** Entity değişikliklerinde Flyway script ekle (`V<timestamp>__description.sql`).
7. **Test ekle.** Yeni servis/endpoint için en az happy path + error case.
8. **OpenAPI güncelle.** Swagger annotation'ları doğru olsun.
9. **Frontend tarafı:** TypeScript tip tanımı `frontend/src/api/*` altında. Yeni endpoint için tip ekle, store/composable yaz, component bağla.
10. **i18n key kullan.** Hard-coded Türkçe metin koyma; `t('appointment.create.success')` gibi.
11. **Boş durum + loading + error** her listelemede olmalı.
12. **Mobil-first responsive.** Tailwind breakpoint: `sm md lg xl 2xl`.
13. **Conventional commits.** `feat(employee-capability): add many-to-many ...`
14. **Türkçe kullanıcı mesajları, İngilizce kod.**
15. **PR'larda checklist:** [ ] Test, [ ] Migration, [ ] OpenAPI, [ ] Frontend, [ ] i18n, [ ] Responsive

---

## 9. Hızlı Başlangıç Komutları

### Backend
```bash
./mvnw spring-boot:run                # H2 + dev
./mvnw spring-boot:run -Pprod         # PostgreSQL
./mvnw test
./mvnw verify
```

### Frontend
```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build
```

### Veritabanı (prod profili)
- `application-prod.properties` içinde `spring.datasource.url=jdbc:postgresql://...`
- `ddl-auto=validate` — şema değişikliği sadece Flyway ile

---

## 10. Mevcut Önemli Dosyalar (referans)

- `md-files/RAKIP-VE-PAZAR-ANALIZI.md` — pazar analizi
- `md-files/STRATEJI-VE-YOL-HARITASI.md` — strateji V1
- `md-files/GELISTIRME-PLANI-V2.md` — modüler geliştirme planı V2
- `md-files/FRONTEND_GEREKSINIM_VE_PLAN.md`
- `md-files/PIPELINE_KALAN_ISLER.md`
- `md-files/api-standards.md`
- `md-files/clean-code.md`
- `md-files/ddd.md`
- `md-files/security.md`
- `md-files/testing.md`
- `PROJE_DURUM_RAPORU.md` — backend ilerleme

---

## 11. Yeni Bir LLM Oturumuna Açılış Talimatı (kopyala-yapıştır)

```
Sen bu projede çalışan bir kıdemli geliştiricisin.
Önce md-files/DEVELOPMENT-CONTEXT.md ve md-files/GELISTIRME-PLANI-V2.md
dosyalarını oku. Şu an Modül {N} üzerinde çalışıyoruz: {modül adı}.

Kurallar:
- Mevcut kod pattern'lerine uy
- Multi-tenant (business_id) izolasyonunu koru
- DTO + mapper kullan, entity dönme
- Bean Validation + service katmanında business rule
- Flyway migration ekle
- Unit + integration test yaz
- Türkçe kullanıcı mesajları, İngilizce kod
- Conventional commits

Görevin: {açıkça yaz}

Önce planı çıkar, onaylayınca uygulamaya başla.
```

---

## 12. Notlar

- WhatsApp Cloud API maliyeti konuşma başınadır; planlara mesaj kotası koyulmalı
- iyzico test ortamı `sandbox-api.iyzipay.com`
- Nilvera test ortamı dökümantasyonu güncel olmalı
- Mobil için Apple Developer ($99/yıl) + Google Play ($25 tek seferlik) hesap maliyetleri var
- Flutter sürümü en az 3.24+
- PostgreSQL 16 önerilir
