# Geliştirme Planı V2 — Modüler Yol Haritası

**Tarih:** 7 Nisan 2026
**Strateji değişikliği:** Abonelik + ödeme sistemi son faza alındı, mobil uygulama ile birlikte yapılacak. Önce ürünü olgunlaştır, müşteri topla, sonra paraya dök.

---

## Faz Sıralaması (Yeni)

| Faz | Kapsam | Süre |
|---|---|---|
| **Faz 1** | Çalışan-hizmet yetkinliği + Çoklu çalışan müsaitlik | 2 hafta |
| **Faz 2** | Modern rezervasyon (public booking) yenileme | 2-3 hafta |
| **Faz 3** | İşletme profili + sosyal medya + galeri | 1 hafta |
| **Faz 4** | Paket yapısı (tek/çoklu çalışan) + fiyatlandırma plan modeli (DB seviye, ödeme yok) | 1 hafta |
| **Faz 5** | Dashboard + analitik + frontend modernizasyonu (PrimeVue/Tailwind) | 3-4 hafta |
| **Faz 6** | E-fatura entegrasyonu (üst plan) | 2 hafta |
| **Faz 7** | Mobil uygulama + Abonelik/Ödeme (iyzico) birlikte | 8-10 hafta |

---

# MODÜL 1 — Çalışan & Hizmet Yetkinliği

## Amaç
Bir işletmede her çalışan her hizmeti veremez. Kuaförde "Mehmet sadece saç keser, Ayşe manikür+pedikür yapar". Müşteri rezervasyon alırken sadece o hizmeti veren çalışanlar listelenmeli, müsaitlik buna göre hesaplanmalı.

## Mevcut Durum
`Employee` ve `ServiceDefinition` entity'leri var; ilişki yok veya zayıf. Müsaitlik tüm çalışanları aynı sayıyor.

## Backend Geliştirme

### 1.1 Veri Modeli
```
EmployeeServiceCapability (Many-to-Many through table)
├── id
├── employee_id (FK)
├── service_id (FK)
├── custom_duration_minutes (nullable — bu çalışan için özel süre)
├── custom_price (nullable — bu çalışan için özel fiyat, kıdem farkı)
├── skill_level (BEGINNER/INTERMEDIATE/EXPERT)
├── active (boolean)
└── created_at
```

`Employee` entity:
- `Set<EmployeeServiceCapability> capabilities`
- `String title` (unvan: Saç Tasarımcısı, Estetisyen)
- `String bio` (kısa tanıtım)
- `String photoUrl`
- `Integer experienceYears`
- `Set<DayOfWeek> workingDays` (bireysel çalışma günleri — işletme genelinden farklıysa)
- `Boolean acceptsOnlineBooking` (true/false)

### 1.2 Servis Katmanı
`EmployeeCapabilityService`:
- `assignService(employeeId, serviceId, customDuration?, customPrice?, skillLevel)`
- `removeCapability(employeeId, serviceId)`
- `getEmployeesByService(serviceId)` → o hizmeti verebilen çalışanlar
- `getServicesByEmployee(employeeId)` → çalışanın verdiği hizmetler
- `getEffectiveDuration(employeeId, serviceId)` → custom varsa custom, yoksa default
- `getEffectivePrice(employeeId, serviceId)`

`AvailabilityService` güncelleme:
- `getAvailableSlots(businessId, serviceId, date, employeeId?)`
- Eğer employeeId verilmişse sadece o çalışanın takvimi
- Verilmemişse o hizmeti verebilen tüm çalışanların birleşik müsaitliği (her slot hangi çalışanlar müsait bilgisiyle)
- Çalışanın `customDuration`'ı varsa onu kullan

### 1.3 API Endpoint'leri
```
POST   /api/v1/employees/{id}/capabilities
DELETE /api/v1/employees/{id}/capabilities/{serviceId}
GET    /api/v1/employees/{id}/capabilities
GET    /api/v1/services/{id}/employees
PUT    /api/v1/employees/{id}  (title, bio, photo, experience, workingDays)

# Public
GET /api/v1/public/{slug}/services/{serviceId}/employees
GET /api/v1/public/{slug}/availability?serviceId=&employeeId=&date=
```

### 1.4 Validasyon Kuralları
- Randevu oluştururken `employeeId` verildiyse o çalışanın o hizmeti verme yetkisi olmalı (`EmployeeServiceCapability` aktif)
- Çalışan silinirken aktif randevuları kontrol et
- Aynı çalışana aynı hizmet bir kez atanabilir

## Frontend Geliştirme
- **Çalışanlar sayfası** → her çalışan kartında "Verdiği Hizmetler" sekmesi
- Hizmet ekleme dialog'u: hizmet seç, isteğe bağlı süre/fiyat override, deneyim seviyesi
- **Hizmetler sayfası** → her hizmet kartında "Veren Çalışanlar" listesi
- Çalışan profili düzenleme: foto upload, unvan, biyo, deneyim yılı

## Test
- Unit: capability assign/remove, effective duration/price hesaplama
- Integration: müsaitlik hesabında kapasite filtresi
- E2E: rezervasyon akışında yanlış çalışan seçimi reddedilmeli

---

# MODÜL 2 — Çoklu Çalışan Paket Yapısı & Fiyatlandırma Modeli

## Amaç
Farklı plan seviyelerinde farklı çalışan limitleri. Düşük plan = 1 çalışan, üst plan = sınırsız. Plan tanımı DB'de tutulacak ama ödeme/abonelik Faz 7'de gelecek; şimdilik manuel atanır.

## Plan Yapısı

| Plan | Çalışan | Aylık Randevu | Hizmet | Şube | E-Fatura | Fiyat (Faz 7'de aktif) |
|---|---|---|---|---|---|---|
| **Başlangıç** (Solo) | 1 | 150 | 10 | 1 | ❌ | **499 TL/ay** |
| **Profesyonel** | 5 | 750 | 30 | 1 | ❌ | **999 TL/ay** |
| **İşletme** | 15 | 2.500 | sınırsız | 3 | ❌ | **1.799 TL/ay** |
| **Premium** | sınırsız | sınırsız | sınırsız | sınırsız | ✅ | **2.999 TL/ay** |

**Rakip karşılaştırma referansı:**
- Planla.co: 1.090 / 1.490 / 2.390 TL
- Kolayrandevu: ~1.199 TL başlangıç
- Randuapp: ~2.750 TL
- SalonRandevu: 1.590 / 2.400 / 3.480 TL

→ **Bizim Başlangıç paketimiz pazarın en ucuzu** (solo profesyoneller için cazip giriş), Profesyonel rakiplerin Plus'ı ile aynı seviyede ama daha çok randevu kotası, Premium e-fatura ile rakiplerden ayrışıyor.

## Backend Geliştirme

### 2.1 Veri Modeli
```
Plan
├── id, code (STARTER/PRO/BUSINESS/PREMIUM)
├── name, description
├── max_employees, max_appointments_monthly, max_services, max_branches
├── features (JSON: ["WHATSAPP", "DASHBOARD", "PACKAGES", "EFATURA", ...])
├── monthly_price, yearly_price (yıllık %20 indirim)
└── active, sort_order

BusinessSubscription (Faz 7'ye kadar manuel atanır, ödeme yok)
├── id
├── business_id (FK, unique)
├── plan_id (FK)
├── status (TRIAL/ACTIVE/EXPIRED/SUSPENDED)
├── trial_end_date
├── current_period_start, current_period_end
├── cancelled_at, ...
└── manual (boolean — Faz 7'de payment ile dolacak)

PlanLimitUsage (her ay yenilenir)
├── business_id, year, month
├── appointments_count, sms_sent, whatsapp_sent
```

### 2.2 Limit Enforcement
`PlanLimitInterceptor` (Spring AOP veya HandlerInterceptor):
- Çalışan oluşturma → max_employees kontrol
- Hizmet oluşturma → max_services
- Randevu oluşturma (PENDING/CONFIRMED) → aylık kota
- Aşımda HTTP 402 + "Plan limitiniz doldu, yükseltin" mesajı

### 2.3 API
```
GET /api/v1/plans                         # public, fiyat sayfası için
GET /api/v1/me/subscription               # mevcut plan
GET /api/v1/me/subscription/usage         # bu ay kullanım
POST /api/v1/admin/businesses/{id}/plan   # admin manuel atar (Faz 7'ye kadar)
```

## Frontend
- **Fiyatlandırma sayfası** (public landing) — 4 plan kartı, özellik karşılaştırma matrisi, yıllık/aylık toggle
- **Ayarlar > Plan ve Kullanım** — mevcut plan, kullanım çubukları (örn. 87/150 randevu), yükselt butonu (Faz 7'ye kadar "yakında")
- Limit aşıldığında uyarı modal'ı

---

# MODÜL 3 — Modern Rezervasyon (Public Booking) Sayfası

## Amaç
Müşterinin link üzerinden randevu aldığı sayfayı 2026 standardına çekmek. Mobil-first, 3 adımlı, görsel zengin, hızlı.

## Kullanıcı Akışı
**Adım 0 (Landing):** İşletme adı, kapak, logo, puan, açıklama, sosyal medya ikonları, "Randevu Al" CTA, hizmet kategori sekmeleri

**Adım 1 — Hizmet Seçimi:**
- Kategoriye göre gruplu kart listesi (foto, ad, süre, fiyat aralığı)
- Çoklu hizmet seçimi (kombo: saç kesim + boyama)
- Toplam süre + toplam fiyat altta sticky bar

**Adım 2 — Çalışan Seçimi (opsiyonel):**
- "Fark etmez" + her çalışan için kart (foto, unvan, deneyim, puan)
- Sadece seçili hizmet(ler)i verebilenler gösterilir

**Adım 3 — Tarih ve Saat:**
- Takvim widget (sonraki 30 gün)
- Müsait günler vurgulu, dolu günler grileşmiş
- Gün seçilince saat çipleri (sabah/öğlen/akşam grupları)
- Anında onay vs Onay bekler etiketleri

**Adım 4 — Bilgiler:**
- Ad soyad, telefon (WhatsApp)
- Otomatik OTP ile WhatsApp doğrulama (no-show'a karşı)
- Notlar
- KVKK onayı
- Özet kart: hizmet(ler), çalışan, tarih/saat, fiyat
- "Randevuyu Onayla" → backend'e POST

**Onay sayfası:**
- ✅ animasyon, randevu özeti
- "Takvime Ekle" (.ics download)
- "WhatsApp'a Hatırlatma Ekle" (zaten otomatik)
- "Konum" (Google Maps link)
- İptal/değiştirme linki (token'lı)

## Backend Eklemeler
- `POST /api/v1/public/{slug}/otp/send` — WhatsApp ile 6 haneli kod
- `POST /api/v1/public/{slug}/otp/verify`
- `POST /api/v1/public/{slug}/appointments` (mevcut, multi-service desteği eklenmeli)
- `Appointment` entity → `services` Many-to-Many (şu an tek hizmet)
- ICS generator (`/api/v1/public/appointments/{token}/calendar.ics`)
- Public iptal endpoint: `POST /api/v1/public/appointments/{token}/cancel`

## Frontend
- Yeni route: `/b/:slug` (Vue)
- Ayrı layout (admin'den bağımsız), tam mobil
- Tailwind + Headless UI veya PrimeVue
- Framer Motion benzeri (vue-motion) ile yumuşak geçişler
- Skeleton loaders, optimistic UI
- A/B test edilebilir yapı

---

# MODÜL 4 — İşletme Profili & Sosyal Medya

## Amaç
İşletme paneline detaylı profil bilgileri, sosyal medya, galeri, konum. Public booking sayfasında ve gelecekte marketplace'te kullanılacak.

## Veri Modeli
`Business` entity'ye eklenecek alanlar:
```
- description (TEXT, max 2000 char)
- logo_url
- cover_image_url
- gallery_images (JSON array, max 20)
- address_line1, address_line2, city, district, postal_code
- latitude, longitude
- google_maps_url
- google_business_url
- email_public
- phone_public  (panel telefonundan farklı, müşteri görür)
- whatsapp_public
- website_url

# Sosyal Medya
- instagram_url
- facebook_url
- tiktok_url
- youtube_url
- x_url (twitter)
- linkedin_url
- threads_url
- pinterest_url

# Diğer
- categories (Many-to-Many → BusinessCategory: Kuaför, Güzellik, Sağlık, vs.)
- amenities (JSON: ["WIFI", "PARKING", "CHILDREN_FRIENDLY", "WHEELCHAIR"])
- payment_methods (JSON: ["CASH", "CREDIT_CARD", "BANK_TRANSFER"])
- accepted_languages (TR, EN, AR)
- founded_year
```

## API
```
GET  /api/v1/me/business            # mevcut, genişletilecek
PUT  /api/v1/me/business
POST /api/v1/me/business/logo       # multipart upload
POST /api/v1/me/business/cover
POST /api/v1/me/business/gallery
DELETE /api/v1/me/business/gallery/{imageId}

# Public
GET /api/v1/public/{slug}/profile
```

## Frontend
- **Ayarlar > İşletme Profili** çok sekmeli sayfa:
  - **Genel:** ad, açıklama, kategori, kuruluş yılı
  - **İletişim:** telefon, email, WhatsApp, web sitesi
  - **Adres:** harita üzerinden pin, şehir/ilçe
  - **Sosyal Medya:** ikonlu input listesi (boş bırakılabilir)
  - **Görseller:** logo, kapak, galeri (drag & drop upload)
  - **Olanaklar:** checkbox listesi (wifi, otopark, vb.)
- Public booking sayfasının header'ında bu bilgiler kullanılır
- Sosyal medya ikonları Lucide / Simple Icons

## Dosya Depolama
- Geliştirme: local `/uploads`
- Production: S3 / Cloudflare R2 / DigitalOcean Spaces
- Spring Boot için `app.storage.provider` config (LOCAL/S3)
- Image resize: ImgScalr veya thumbnailator (3 boyut: thumb 150, medium 600, full 1600)

---

# MODÜL 5 — Dashboard & Analitik + Frontend Modernizasyon

## Frontend Stack Yenileme
- **Tailwind CSS 4** kurulum
- **PrimeVue 4** (tablo, takvim, form, dialog hazır)
- **Lucide Vue** ikonlar
- **vee-validate + zod** form validation
- **vue-sonner** toast
- **ApexCharts Vue** grafik
- **FullCalendar Vue** takvim
- **vue-i18n** TR/EN
- **Pinia persisted state**

## Tasarım Sistemi
- Renk paleti (primary, success, warning, danger, neutral)
- Tipografi (Inter veya Poppins)
- Spacing scale, border radius, shadow
- Bileşen kütüphanesi: Button, Card, Input, Select, Modal, Table, EmptyState, Skeleton, Toast

## Dashboard Sayfası
- **KPI kartları:** Bugün randevu, Bu ay gelir tahmini, Doluluk %, No-show oranı, Yeni müşteri
- **Grafikler:**
  - 30 günlük randevu trendi (line)
  - Çalışan bazlı doluluk (bar)
  - Hizmet bazlı gelir dağılımı (pie/donut)
  - Saat ısı haritası (en yoğun saatler)
- **Yaklaşan randevular widget'ı**
- **Risk uyarısı widget'ı** (RISKY status)

## Backend Endpoint'leri
```
GET /api/v1/dashboard/overview?range=today|week|month
GET /api/v1/dashboard/revenue-trend?days=30
GET /api/v1/dashboard/employee-occupancy
GET /api/v1/dashboard/service-revenue
GET /api/v1/dashboard/hourly-heatmap
GET /api/v1/dashboard/no-show-rate
GET /api/v1/dashboard/upcoming-appointments
```

## Yenilenecek Sayfalar
- Login/Register (modern, sosyal hesap girişi yer tutucu)
- Onboarding wizard (5 adım: işletme → çalışma saatleri → hizmetler → çalışanlar → davet linki)
- Takvim (FullCalendar — gün/hafta/ay, sürükle-bırak, çalışan filtresi, renk-status)
- Randevular (tablo + kanban toggle)
- Müşteriler (tablo + müşteri detay drawer: geçmiş, paket, not)
- Hizmetler, Çalışanlar, Paketler — modern card+table view
- Ayarlar (profil, plan, bildirimler, WhatsApp bağlantısı)

---

# MODÜL 6 — E-Fatura (Premium Plan)

## Amaç
En üst plan müşterileri için e-Fatura/e-Arşiv otomatik kesimi. Türkiye'de zorunlu, rakiplerden Randuapp'ta var.

## Sağlayıcı Seçenekleri
- **Foriba** (en yaygın)
- **Logo e-Fatura**
- **Nilvera**
- **GİB Portal direkt** (zor, önerilmez)

→ Önerilen: **Nilvera** (REST API, dökümantasyon iyi, fiyat uygun)

## Veri Modeli
```
Invoice (mevcut Subscription/Payment ile karıştırılmasın — bu müşteri faturası)
├── id
├── business_id
├── customer_id
├── appointment_id (nullable)
├── invoice_number
├── invoice_date
├── total_amount, vat_amount, net_amount
├── status (DRAFT/SENT/PAID/CANCELLED)
├── e_invoice_uuid (sağlayıcıdan)
├── e_invoice_status (PENDING/SENT/ACCEPTED/REJECTED)
├── pdf_url, xml_url
└── ...

InvoiceItem
├── description, quantity, unit_price, vat_rate, total
```

## İş Akışı
1. Randevu COMPLETED olduğunda otomatik fatura draft'ı oluşturulur (premium plan ise)
2. İşletme onaylar / düzenler
3. Nilvera API'ye gönder
4. Sağlayıcıdan UUID + PDF/XML alınır
5. Müşteriye e-mail/WhatsApp ile gönderilir

## API
```
POST /api/v1/invoices
GET  /api/v1/invoices?from=&to=
POST /api/v1/invoices/{id}/send-eInvoice
GET  /api/v1/invoices/{id}/pdf
POST /api/v1/invoices/{id}/cancel
```

## Frontend
- **Faturalar** sayfası (sadece Premium plan görür)
- Liste, filtre, durum
- Yeni fatura (manuel) modal'ı
- E-fatura ayarları (Nilvera kimlik bilgileri, ETN, varsayılan KDV)
- Fatura PDF preview

## Plan Kontrol
`@RequiresPlan(Plan.PREMIUM)` annotation veya feature flag kontrol

---

# MODÜL 7 — Mobil Uygulama + Abonelik/Ödeme (BİRLİKTE)

Bu faz ikiye ayrılır ama paralel ilerler. Mobil çıkarken iyzico ödeme de hazır olmalı (uygulamadan abonelik başlatılabilsin).

## 7A — Mobil Uygulama

### Teknoloji: **Flutter** (önerilen)
**Neden:**
- Tek kod tabanı (iOS + Android)
- Yüksek performans
- Native takvim, push, biometric kolay
- Web ekibinden bağımsız ilerler

**Alternatif:** React Native + Expo (web ekibi JS biliyorsa)

### İki Uygulama
1. **İşletme uygulaması** (panel) — günlük operasyon
2. **Müşteri uygulaması** (light) — randevu al, geçmiş, paket bakiye, hatırlatma

### İşletme Uygulaması Özellikleri
- Login (email + şifre + biometrik)
- Bugünün takvimi (öncelik)
- Randevu ekle/düzenle/iptal
- Müşteri arama + ekleme
- Müşteri detay (geçmiş, paket)
- Push: yeni randevu, iptal, RISKY
- Hızlı eylemler: randevuyu COMPLETED işaretle, paket seans düş
- Çevrimdışı önbellek (son 7 gün)
- WhatsApp hızlı mesaj
- Plan yükselt / fatura

### Müşteri Uygulaması Özellikleri
- İşletme arama (slug veya QR)
- Favori işletmeler
- Randevu al (web flow'unun mobile native versiyonu)
- Aktif/geçmiş randevular
- Paket bakiyem
- Hatırlatma ayarları
- Push bildirim (T-24, T-2)
- Konum + harita
- Değerlendirme yıldız + yorum

### Backend Eklemeler
```
POST /api/v1/devices/register        # FCM token kaydı
DELETE /api/v1/devices/{id}
POST /api/v1/notifications/push      # internal
GET /api/v1/customer-app/businesses/search?q=
```

FCM (Firebase Cloud Messaging) entegrasyonu — Spring Boot tarafında `firebase-admin` SDK.

### Mobil Sprint Planı
| Sprint | Süre | Çıktı |
|---|---|---|
| 0 | 1 hafta | Figma tasarım sistemi, kullanıcı akışları |
| 1 | 1 hafta | Flutter proje kurulum, auth, navigation |
| 2 | 2 hafta | İşletme: dashboard, takvim, randevu CRUD |
| 3 | 1 hafta | İşletme: müşteri, paket, push |
| 4 | 2 hafta | Müşteri uygulaması: arama, randevu al, geçmiş |
| 5 | 1 hafta | Native takvim sync, biometrik, offline |
| 6 | 1 hafta | iyzico abonelik akışı uygulama içi |
| 7 | 1 hafta | Beta test, store yayını |

## 7B — Abonelik & Ödeme (iyzico)

### Sağlayıcı: **iyzico**
- TR pazar standardı
- 3D Secure
- Tekrarlayan ödeme (subscription)
- Kart saklama (token)
- Webhook
- Test ortamı iyi

### Veri Modeli
```
Subscription (Modül 2'deki yapı genişletilir)
├── id, business_id, plan_id
├── status (TRIAL/ACTIVE/PAST_DUE/CANCELLED/EXPIRED)
├── billing_cycle (MONTHLY/YEARLY)
├── trial_end_date
├── current_period_start, current_period_end
├── next_billing_date
├── iyzico_subscription_reference
├── cancelled_at, cancel_reason
└── ...

PaymentMethod
├── id, business_id
├── iyzico_card_token
├── card_last_four, card_brand, expire_month/year
├── is_default
└── ...

Payment
├── id, business_id, subscription_id, invoice_id
├── amount, currency, status (PENDING/SUCCESS/FAILED/REFUNDED)
├── iyzico_payment_id, conversation_id
├── failure_reason
└── ...

SubscriptionInvoice (faturalama, e-fatura'dan farklı)
├── id, subscription_id, period_start, period_end
├── amount, status, paid_at
└── ...
```

### İş Akışı
1. **14 gün ücretsiz deneme** (kart istemeden)
2. Deneme bitiminde plan seç + kart ekle
3. iyzico subscription oluştur
4. Webhook ile ödeme başarılı/başarısız işle
5. Başarısız → 3 deneme + e-mail/WhatsApp uyarı → suspend

### API
```
POST /api/v1/billing/checkout                  # plan + billing cycle seç
POST /api/v1/billing/payment-methods           # 3DS kart ekleme
GET  /api/v1/billing/payment-methods
DELETE /api/v1/billing/payment-methods/{id}
POST /api/v1/billing/subscription/upgrade
POST /api/v1/billing/subscription/cancel
POST /api/v1/billing/subscription/reactivate
GET  /api/v1/billing/invoices
GET  /api/v1/billing/invoices/{id}/pdf
POST /api/v1/billing/iyzico/webhook
```

### Frontend
- Plan seçim sayfası (yıllık %20 indirim toggle)
- 3DS kart ekleme formu (iyzico iframe veya kendi UI)
- Fatura geçmişi
- Plan yükselt/iptal
- Trial countdown banner

### Güvenlik
- Webhook imza doğrulama
- PCI uyumluluk: kart bilgisi asla bizim sunucuya değmez (iyzico tokenize)
- Plan limit interceptor (Modül 2'deki yapı aktifleşir)

---

# Geliştirme Sırası ve Bağımlılıklar

```
Modül 1 (Çalışan-Hizmet)  ──┐
                            ├──> Modül 3 (Modern Booking)
Modül 4 (Profil/Sosyal)  ───┘

Modül 2 (Plan modeli)    ──> Modül 5 (Dashboard) ──> Modül 6 (E-Fatura)
                                                  ──> Modül 7 (Mobil + Ödeme)
```

**Önerilen sıra:** 1 → 4 → 3 → 2 → 5 → 6 → 7

Her modül bağımsız PR olarak geliştirilebilir.

---

# Kalite ve DevOps Kontrol Listesi (her modül için)

- [ ] Entity + Flyway migration
- [ ] Repository + Service + Controller + DTO + Mapper
- [ ] Validation (Bean Validation + business rules)
- [ ] Multi-tenant (business_id) izolasyon
- [ ] Unit test (>%70 coverage)
- [ ] Integration test (kritik akışlar)
- [ ] OpenAPI/Swagger güncel
- [ ] Postman koleksiyonu
- [ ] Frontend sayfa + responsive
- [ ] i18n (TR/EN)
- [ ] Boş durum + loading + error
- [ ] Code review checklist
- [ ] CHANGELOG güncel

---

*Detaylı geliştirme context'i için bkz. `DEVELOPMENT-CONTEXT.md`.*
