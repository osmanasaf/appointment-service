# Frontend Gereksinim Analizi ve Teknik Plan

**Tarih:** 5 Mart 2026  
**Pipeline:** Gereksinim → Bağlam → Kurallar → Plan → Görevler (+ Web Interface Guidelines)

---

## 1. Gereksinim Analizi (Requirement)

### 1.1 Ana İstekler (Kullanıcıdan)

| # | İstek | Öncelik |
|---|--------|--------|
| R1 | **Admin paneli** — Tek kullanıcı (işletme) yönetimi | Must |
| R2 | **Hizmetler / Paketler** tanımlama | Must |
| R3 | **Müşteri ve telefon** tanımlama | Must |
| R4 | **Müşteriye paket atama** | Must |
| R5 | **Çalışan tanımlama**, çalışanların **yetkileri** | Must |
| R6 | **Public URL** — İşletmenin müsait saatlerini gösterme, müşterinin randevu alabilmesi | Must |
| R7 | Basit tutulacak; ek özellikler planlanabilir | Should |

### 1.2 Yapılandırılmış Gereksinimler

**Admin (İşletme Paneli)**

- **Auth:** Giriş (login) / Kayıt (register) — backend JWT.
- **İşletme:** Profil ve çalışma saatleri (backend’de business + working hours).
- **Hizmetler:** CRUD — ad, süre, buffer, onay gerekli mi, paket uygun mu (backend: `ServiceDefinition`).
- **Paketler:** Paket tanımı (seans sayısı, hangi hizmete bağlı); müşteriye paket atama / seans düşme (backend: `CustomerPackage`, `PackageController`).
- **Müşteriler:** CRUD — ad, telefon (backend: `Customer`, `CustomerController`).
- **Müşteriye paket atama:** Müşteri detayında paket ekleme / listeleme (backend: `GET /customers/{id}/packages`, paket ekleme/deduct/add-sessions).
- **Çalışanlar:** CRUD — ad, işletmeye bağlı (backend: `Employee`). Çalışan **yetkileri:** Backend’de `User` + `UserRole` (ADMIN, BUSINESS_OWNER, EMPLOYEE); çalışan için giriş hesabı ve rol atama.
- **Randevular:** Listeleme, onaylama, iptal, tamamlama, no-show (backend: `AppointmentController`).

**Public (Müşteri Tarafı)**

- **Tek URL:** İşletmeye özel link (slug). Örn: `/randevu/{slug}` veya `/b/{slug}`.
- **İçerik:** İşletme adı, müsait saatler (çalışma saatleri + müsait slotlar).
- **Akış:** Hizmet seç → Çalışan seç (veya “fark etmez”) → Tarih seç → Saat seç → Ad, telefon, not → Randevu oluştur.
- **Backend:** `GET /api/v1/public/businesses/{slug}`, `.../employees`, `.../services`, `GET availability`, `GET availability/dates`, `POST .../appointments`.

### 1.3 Kapsam Dışı (Bu Fazda)

- WhatsApp webhook UI, SMS ayarları.
- Çoklu işletme / süper admin (ADMIN rolü ileride genişletilebilir).
- Mobil native uygulama.

---

## 2. Bağlam (Backend API Özeti)

### 2.1 Base URL ve Auth

- **Base:** `http://localhost:8080/api/v1` (veya env’den).
- **Auth:** `POST /auth/register`, `POST /auth/login` → JWT. Koruma altındaki endpoint’lere `Authorization: Bearer <token>`.

### 2.2 Koruma Altındaki API’ler (Admin)

| Alan | Endpoint | Açıklama |
|------|----------|----------|
| İşletme | `GET/POST /businesses`, `GET/PUT /businesses/{id}`, `GET /businesses/slug/{slug}` | |
| Çalışan | `GET/POST /employees`, `GET /employees/{id}`, `POST /employees/{id}/activate|deactivate` | |
| Hizmet | `GET/POST /services`, `GET /services/{id}`, `POST /services/{id}/activate|deactivate` | |
| Müşteri | `GET/POST /customers`, `GET /customers/{id}`, `GET /customers/{id}/packages`, blacklist | |
| Paket | `GET/POST /packages`, `GET /packages/{id}`, deduct, add-sessions | |
| Randevu | `GET/POST /appointments`, `GET /appointments/{id}`, confirm, cancel, complete, no-show, risky | |

### 2.3 Public API (Token gerektirmez)

| Metod | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/public/businesses/{slug}` | İşletme bilgisi |
| GET | `/public/businesses/{slug}/employees` | Aktif çalışanlar |
| GET | `/public/businesses/{slug}/services` | Aktif hizmetler |
| GET | `/public/availability?businessId&employeeId&serviceId&date` | Slot listesi |
| GET | `/public/availability/dates?businessId&employeeId&serviceId&daysAhead=30` | Müsait tarihler |
| POST | `/public/businesses/{slug}/appointments` | Body: employeeId, serviceId, scheduledAt, customerName, phoneNumber, phoneCountryCode?, notes? |

### 2.4 Response Formatı

- Başarı: `{ "success": true, "data": { ... }, "message": "..." }`
- Hata: `{ "success": false, "error": { "code", "message", "timestamp" } }`

---

## 3. Proje Kuralları (Özet)

- **Kod:** İngilizce (sınıf, metod, değişken, yorum).
- **API:** Thin controller; DTO kullanımı; tutarlı error formatı.
- **Güvenlik:** Input validation; hassas endpoint’lerde yetki; CORS sadece gerekli origin.
- **DRY / Clean:** Tekrarları util’e taşı; anlamlı isimler; magic number yok.
- **Frontend (Web Interface Guidelines):** Erişilebilirlik (aria-label, semantic HTML, focus), form kuralları (label, autocomplete, hata mesajı), animasyon (prefers-reduced-motion), tipografi (…), URL ile state, destructive action’da onay.

---

## 4. Teknik Plan (Frontend)

### 4.1 Stack Önerisi

- **Framework:** Vue 3 (Composition API) + TypeScript.
- **Build:** Vite.
- **Routing:** Vue Router (admin + public ayrı rotalar).
- **State:** Pinia (auth, işletme bilgisi, basit cache).
- **HTTP:** Axios veya fetch + base URL + JWT interceptor.
- **UI:** Basit bileşenler; Tailwind CSS ile stil (utility-first, erişilebilir focus/hover).
- **Tarih/saat:** `Intl.DateTimeFormat` (guidelines); slotlar için backend’den gelen ISO string’ler.

### 4.2 Rota Yapısı

- **`/login`**, **`/register`** — Auth (public).
- **`/admin`** (layout: sidebar + header)  
  - `/admin` — Dashboard (özet kartlar).  
  - `/admin/business` — İşletme profil / çalışma saatleri.  
  - `/admin/services` — Hizmet listesi + ekleme/düzenleme.  
  - `/admin/packages` — Paket listesi + tanım.  
  - `/admin/customers` — Müşteri listesi + detay (paket atama).  
  - `/admin/employees` — Çalışan listesi + yetki/rol.  
  - `/admin/appointments` — Randevu listesi (onay/iptal/tamamla/no-show).
- **`/b/:slug`** veya **`/randevu/:slug`** — Public randevu sayfası (tek URL; müsaitlik + form).

### 4.3 Fazlar

| Faz | İçerik | Çıktı |
|-----|--------|--------|
| F1 | Proje iskeleti, auth (login/register), API client, router, Pinia auth | Giriş yapıp token alma |
| F2 | Admin layout, işletme + hizmet + paket + müşteri + çalışan CRUD, müşteriye paket atama | Tam admin paneli |
| F3 | Çalışan yetkileri (rol gösterimi / atama — backend’de User-Employee ilişkisi) | Yetki yönetimi |
| F4 | Randevu listesi, onay/iptal/tamamla/no-show aksiyonları | Randevu yönetimi |
| F5 | Public sayfa: slug ile işletme, hizmet/çalışan seçimi, tarih/saat, form, POST randevu | Müşteri randevu alma |

---

## 5. Görev Listesi (Task’lar)

### F1 – İskelet ve Auth
1. Vue 3 + Vite + TypeScript projesi oluştur.
2. Vue Router kur; `/login`, `/register`, `/admin/*`, `/b/:slug` rotaları.
3. Axios (veya fetch) + base URL + 401’de login’e yönlendirme.
4. Pinia: `useAuthStore` (token, user, login, logout).
5. Login ve Register sayfaları (form, validation, hata mesajı).
6. Admin layout (sidebar menü: İşletme, Hizmetler, Paketler, Müşteriler, Çalışanlar, Randevular).
7. Auth guard: `/admin/*` için token yoksa `/login`.

### F2 – Admin CRUD
8. İşletme: form (ad, slug, çalışma saatleri) + kaydet.
9. Hizmetler: liste + ekle/düzenle (ad, süre, buffer, onay gerekli, paket uygun).
10. Paketler: liste + ekle (ad, seans sayısı, hizmet).
11. Müşteriler: liste + ekle/düzenle (ad, telefon); detayda paket listesi + “Paket ata”.
12. Çalışanlar: liste + ekle/düzenle (ad, vb.); (F3’te rol bağlanacak).

### F3 – Yetkiler
13. Çalışan detayında kullanıcı hesabı (email) ve rol (BUSINESS_OWNER / EMPLOYEE) alanları (backend’e uyumlu endpoint varsa).

### F4 – Randevu Yönetimi
14. Randevu listesi (tarih, müşteri, hizmet, çalışan, durum).
15. Onay / İptal / Tamamla / No-show butonları + onay modal.

### F5 – Public Randevu
16. `/b/:slug` sayfası: GET business, services, employees.
17. Adımlar: Hizmet seç → Çalışan seç → Tarih seç (availability/dates) → Saat seç (availability) → Form (ad, telefon, not) → POST appointment.
18. Başarı: “Randevunuz alındı” mesajı.

### Web Interface Guidelines (Tüm Fazlar)
- Form: label, autocomplete, aria-label (icon butonlar), hata inline.
- Loading: “Yükleniyor…” (… karakteri).
- Tarih/sayı: `Intl` kullanımı.
- Destructive: onay modal.
- Focus visible; semantic HTML; boş state’ler.

---

## 6. Sonraki Adım

Bu doküman onaylandıktan sonra **F1** ile frontend projesi oluşturulacak (Vue 3 + Vite + TypeScript, auth, router, admin layout iskeleti).
