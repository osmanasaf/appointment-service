# Randevu SaaS — Strateji, Eksik Analizi ve Yol Haritası

**Tarih:** 7 Nisan 2026
**Hazırlayan:** Ürün/Teknik analiz
**Kapsam:** Mevcut projenin (appointment-service) ticarileştirilmesi, rakiplere (planla.co, kolayrandevu, randuapp, asistline) karşı konumlama, eksik özelliklerin kapatılması, frontend kullanıcı dostu hale getirme ve mobil uygulama planı.

---

## 1. Yönetici Özeti

Backend MVP %90 hazır. Çekirdek farklılaştırıcılarımız rakiplerde yok:
- **No-show / RISKY randevu otomasyonu** (T-24 onay + risk skorlama)
- **İşlem süresi bazlı (15 dk – 6 sa) + buffer'lı akıllı müsaitlik** (rakipler hâlâ slot bazlı)
- **Paket/seans entegre randevu** (otomatik seans düşümü)
- **WhatsApp-first** (gönderim + iki yönlü altyapı)
- **Domain-Driven** mimari → hızlı özellik eklenebilir taban

Para kazanmak için kritik 4 eksik:
1. **Abonelik + ödeme altyapısı** (iyzico/Paratika/Stripe TR)
2. **İşletme dashboard'u + analitik** (gelir, doluluk, no-show oranı, müşteri LTV)
3. **Modern, kullanıcı dostu frontend** (mevcut Vue iskeleti var ama UX/tasarım eksik)
4. **Native mobil veya güçlü PWA** (push, takvim, müşteri uygulaması)

Pazara giriş stratejisi: **"Gelmeyen randevuları %40 azaltan WhatsApp randevu sistemi"** mesajı + 14 gün ücretsiz deneme + 599 TL/ay başlangıç fiyatı.

---

## 2. Mevcut Proje Tarama Sonuçları

### 2.1 Backend (Spring Boot 4 / Java 21)
**Var olanlar:**
- Auth (JWT), Business/Employee/Customer/Service/Appointment/Package CRUD
- AvailabilityService (süre + buffer + çalışma saatleri)
- AppointmentStatus state machine: PENDING/CONFIRMED/RISKY/CANCELLED/COMPLETED/NO_SHOW
- Public booking API (slug ile)
- WhatsApp NotificationService + ReminderScheduler (T-24)
- WebhookController (verify ✓, mesaj/buton işleme kısmi)
- Domain value objects (PhoneNumber, TimeSlot, Money, ServiceDuration)
- PostgreSQL prod profili, CORS config

**Eksikler:**
- Multi-tenant izolasyon sertleştirme (her sorguda business_id zorunluluğu)
- Webhook HMAC imza doğrulama
- Rate limiting (Bucket4j)
- Subscription/Billing modülü (Plan, Subscription, Invoice, Payment)
- Audit log
- Gerçek SMS provider impl (Netgsm/Twilio)
- Test kapsamı (entegrasyon testleri)

### 2.2 Frontend (Vue 3 + Vite + Pinia)
**Var olanlar:** Admin layout, login/register, dashboard, appointments, customers, employees, services, packages, business, public book sayfası iskelet halinde.

**Eksikler:**
- Modern UI kit yok (Tailwind / shadcn-vue / Naive UI / PrimeVue önerilir)
- Boş/iskelet sayfalar — gerçek tablolar, formlar, validasyon, boş durumlar yok
- Takvim görünümü (FullCalendar / vue-cal) yok
- Onboarding wizard yok
- Mobil responsive tasarım yok
- i18n (TR/EN) yok
- Bildirim/toast sistemi yok
- Dashboard grafikleri (ApexCharts/Chart.js) yok
- Public booking sayfası tasarım/UX zayıf

---

## 3. Rakip Konumlama Matrisi (özet)

| Kriter | Bizim Avantaj | Rakip Avantajı |
|---|---|---|
| WhatsApp iki yönlü onay/iptal | ✅ (tamamlanmalı) | Asistline kısmen |
| Süre + buffer akıllı müsaitlik | ✅ Tek biz | — |
| No-show / risk skoru | ✅ Tek biz | — |
| Paket-seans entegre | ✅ | Kolayrandevu/Planla kısmi |
| Online ödeme / depozito | ❌ | Planla, Randuapp, Asistline |
| Abonelik billing | ❌ | Hepsi |
| Dashboard/analitik | ❌ | Hepsi |
| Native mobil | ❌ | Kolayrandevu, SalonRandevu |
| Çoklu şube | ❌ | Kolayrandevu, SalonRandevu |
| Google Takvim | ❌ | Planla, Randuapp, Asistline |
| Tekrarlayan randevu | ❌ | Planla |
| E-fatura | ❌ | Randuapp |

**Sonuç:** Para kazanma kapısını açan ödeme/abonelik + dashboard yapılırsa, no-show + WhatsApp + akıllı müsaitlik üçlüsüyle premium niş kazanılabilir.

---

## 4. Faz 1 — Gelir Üretmeye Hazır (4-6 hafta)

Hedef: İlk 50 ödeme yapan müşteri.

### 4.1 Backend
1. **Subscription modülü**
   - Entity'ler: `Plan`, `Subscription`, `Invoice`, `PaymentTransaction`
   - State: TRIAL → ACTIVE → PAST_DUE → CANCELLED
   - Endpoint: `/api/v1/billing/plans`, `/subscriptions`, `/invoices`
2. **Ödeme entegrasyonu** — iyzico (öncelik) veya Paratika
   - 3D Secure, tekrarlayan ödeme, kart saklama
   - Webhook → invoice paid → subscription extend
3. **Plan limit enforcement** — interceptor: aylık randevu sayısı, çalışan sayısı, WhatsApp mesaj kotası
4. **Multi-tenant guard** — her repository'de `business_id` filter (Hibernate filter veya Specification base)
5. **Webhook HMAC imza** doğrulama (Meta WhatsApp X-Hub-Signature-256)
6. **WhatsApp two-way işleme tamamla** — "Geliyorum"/"İptal"/"Erteleme" buton callback'leri → AppointmentStatus geçişleri
7. **Audit log** (Hibernate Envers)
8. **Rate limit** Bucket4j (public + webhook)
9. **Dashboard veri endpoint'leri**: `/api/v1/dashboard/overview`, `/revenue`, `/occupancy`, `/no-show-rate`, `/top-services`, `/customer-ltv`

### 4.2 Frontend (modern UX)
1. **UI kit seçimi** — **PrimeVue 4 + Tailwind** (tablo, takvim, form, dashboard hazır bileşenler)
2. **Tasarım sistemi** — renk paleti, tipografi, spacing, ikonlar (Lucide)
3. **Onboarding wizard** (5 adım):
   - İşletme bilgileri → Çalışma saatleri → Hizmetler → Çalışanlar → WhatsApp bağlama
4. **Ana sayfalar yenileme**
   - Dashboard: KPI kartları (bugün randevu, gelir, doluluk %, no-show %), 7-30 günlük grafik, yaklaşan randevular widget'ı
   - **Takvim sayfası** (FullCalendar Vue) — gün/hafta/ay, sürükle-bırak, çalışan filtresi, renkli durumlar
   - Müşteriler: arama, etiket, paket durumu, geçmiş randevu
   - Hizmetler: süre/buffer/fiyat editörü, paket eşleştirme
   - Paketler: oluştur, müşteriye ata, kalan seans
   - Randevular: liste + kanban (PENDING/CONFIRMED/RISKY/COMPLETED)
   - Ayarlar > Abonelik > Plan değiştir / fatura geçmişi / kart yönetimi
5. **Public booking sayfası yenile** — mobil-first, 3 adım (Hizmet → Tarih/Saat → Bilgi & onay), WhatsApp doğrulama
6. **Bildirim/toast sistemi** (vue-sonner)
7. **i18n** (vue-i18n) — TR varsayılan, EN hazır
8. **Boş durum + skeleton loader** her sayfada
9. **Responsive** breakpoints (mobil/tablet/desktop)
10. **Form validation** (vee-validate + zod)

### 4.3 Pazarlama / Go-to-market
- Landing page: planla.co tarzı, "no-show'ları azaltıyoruz" mesajı, fiyat tablosu, demo video
- Fiyatlandırma: 14 gün ücretsiz, **Başlangıç 599 TL** (1 çalışan, 100 randevu/ay), **Profesyonel 999 TL** (5 çalışan, 500 randevu), **İşletme 1.699 TL** (sınırsız, çoklu şube — Faz 2'de açılır)
- İlk müşteriler: 30 güzellik salonu/psikolog/diyetisyen ile ücretsiz pilot
- Karşılaştırma sayfası: "Bizden vs Planla vs Kolayrandevu"

---

## 5. Faz 2 — Rekabet Eşitleme (6-10 hafta)

1. **Online ödeme / depozito** — müşteri randevu alırken ön ödeme/depozito (no-show'a karşı en güçlü silah)
2. **Google Takvim 2-yönlü senkron**
3. **Tekrarlayan randevu** (haftalık/aylık)
4. **Tutorial/help center** + in-app onboarding
5. **Native mobil — React Native veya Flutter** (bkz. Bölüm 7)
6. **Müşteri uygulaması (light)** — randevu al, geçmiş, paket bakiye, hatırlatmalar
7. **Gelişmiş raporlar** — saatlik doluluk ısı haritası, çalışan performans, hizmet karlılık
8. **Müşteri segmentasyonu + WhatsApp toplu mesaj** (kampanya)
9. **Otomatik yorum/Google Review isteği** — randevu sonrası

---

## 6. Faz 3 — Ölçekleme (10-20 hafta)

- Çoklu şube + merkezi raporlama
- Stok / ürün satışı
- e-Fatura / e-Arşiv (Foriba/Logo)
- Personel maaş + komisyon
- Marketplace (planla.co'nun hizmet keşfi gibi)
- White-label / API erişimi
- AI no-show tahmini (geçmiş veriden risk skoru)
- Otomatik fiyatlandırma önerisi (yoğun saat dinamik fiyat)

---

## 7. Mobil Uygulama Planı

### Strateji
İki uygulama:
- **İşletme uygulaması** (panel) — günlük operasyon
- **Müşteri uygulaması** — randevu al, hatırlatma, paket bakiye

### Teknoloji Önerisi: **Flutter** (önerilen) veya React Native
**Neden Flutter:** Tek kod tabanı, Vue/web ekibinden bağımsız, performans, push, native takvim entegrasyonu kolay. Alternatif: React Native + Expo (web ekibi JS biliyorsa).

**Hızlı alternatif (4-6 hafta):** Mevcut Vue frontend'i **PWA + Capacitor** ile sarıp Play/App Store'a koymak. Push, kamera, takvim erişimi sınırlı ama hızlı.

### Yol Haritası
**Sprint 0 (1 hafta):** Tasarım sistemi (Figma) + kullanıcı akışları
**Sprint 1-2:** Auth, dashboard, takvim, randevu CRUD (işletme uygulaması)
**Sprint 3:** Müşteri/paket/hizmet yönetimi, push (FCM)
**Sprint 4:** Müşteri uygulaması — public booking, geçmiş, paket
**Sprint 5:** Native takvim sync, biometrik giriş, offline cache
**Sprint 6:** Beta test, store yayını

**Ortak backend:** Mevcut REST API yeterli. Eklenecek: device token kaydı, push notification servisi (FCM), uygulama ayarları endpoint'i.

---

## 8. Öne Geçeceğimiz 5 Net Konumlama

1. **"Gelmeyen randevulara son"** — RISKY status + T-24 otomatik onay + depozito ile pazar lideri olabiliriz
2. **"Süre senin, slot değil"** — 15 dk - 6 sa esnek hizmet süreleri (rakipler 30/60 sabit)
3. **"WhatsApp'tan yöneten tek panel"** — iki yönlü onay/iptal/erteleme
4. **"Paket sattın mı, seansı biz sayarız"** — paket+randevu otomatik entegrasyon
5. **"5 dakikada kuruluma hazır"** — onboarding wizard + örnek veri + yardım merkezi

---

## 9. Riskler ve Karşı Hamleler

| Risk | Karşı hamle |
|---|---|
| Rakipler 10+ yıldır pazarda | Niş başla (psikolog/diyetisyen), referans topla |
| WhatsApp Business API maliyeti | Konuşma başına ücret modelini plana yansıt; SMS fallback |
| Ödeme entegrasyonu zorluğu | iyzico Türkiye standardı, dokümantasyon iyi |
| Mobil geliştirme maliyeti | Faz 2'ye ertele; önce PWA + Capacitor |
| Müşteri kazanım maliyeti | Pilot programı + içerik pazarlama (SEO blog) |

---

## 10. Aksiyon Listesi (önümüzdeki 2 hafta)

- [ ] PrimeVue + Tailwind kurulumu, tasarım sistemi
- [ ] Onboarding wizard
- [ ] Dashboard endpoint'leri + grafikler
- [ ] FullCalendar entegrasyonu
- [ ] Public booking sayfası yeniden tasarım
- [ ] iyzico hesabı + Subscription/Plan/Invoice entity şeması
- [ ] WhatsApp webhook two-way tamamla + HMAC imza
- [ ] Multi-tenant filter sertleştirme
- [ ] Landing page taslağı
- [ ] 10 pilot işletme listesi

---

*Detaylı rakip verileri için bkz. `RAKIP-VE-PAZAR-ANALIZI.md`.*
