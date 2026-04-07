# Agent Dağılımı & Paralel Çalışma Planı

**Tarih:** 7 Nisan 2026
**Hedef:** 3 AI agent (Claude Code, Codex CLI, Cursor) paralel olarak farklı modüllerde çalışsın.

---

## Genel Strateji

```
         WAVE 1 (Aynı Anda Başla)
         ┌────────────────────────────────────┐
         │                                    │
   Claude Code          Codex CLI           Cursor
   [Modül 1]           [Modül 4]          [Frontend
 Çalışan-Hizmet       Plan Modeli         Modernizasyonu
  Yetkinliği          (DB + Limit)        Başlangıcı]
         │                                    │
         └──────────────┬─────────────────────┘
                        ↓
         WAVE 2 (Wave 1 tamamlanınca)
         ┌────────────────────────────────────┐
         │                                    │
   Claude Code          Codex CLI           Cursor
   [Modül 3]          [Test Yazımı         [Modül 3
  Public Booking      + Migrations]        Frontend
   Backend]                               + Modül 4
                                          Frontend]
         └──────────────┬─────────────────────┘
                        ↓
         WAVE 3
   Claude Code → Dashboard API (Modül 5 backend)
   Cursor      → Dashboard Frontend + Grafikler
   Codex       → E-Fatura backend (Modül 6)
```

### Branch stratejisi
- `feature/employee-capability` → Claude Code
- `feature/plan-model` → Codex
- `feature/frontend-modernization` → Cursor (frontend branch)
- Merge sırası: Modül 4 → Modül 1 → Frontend (bağımlılık yok)

---

## CLAUDE CODE — Görevi

### Platform: Claude Code CLI (terminal)
### Branch: `feature/employee-capability`
### Skill'ler: `battle-plan`, `development-pipeline`, `refactor-java`, `test-writer`, `context-handoff`
### Kural dosyaları: `CLAUDE.md` (kuruldu, 18 skill + tüm backend kuralları)

### Modül 1: Çalışan-Hizmet Yetkinliği (WAVE 1)

**Yapılacaklar:**

#### Backend
1. `EmployeeServiceCapability` entity oluştur
   - `employee_id`, `service_id` FK, `custom_duration_minutes`, `custom_price`, `skill_level` (ENUM), `active`
   - Many-to-Many junction table: `employee_service_capability`
2. `Employee` entity güncelle: `title`, `bio`, `photoUrl`, `experienceYears`, `acceptsOnlineBooking` alanları ekle
3. Flyway migration: `V{timestamp}__add_employee_service_capability.sql`
4. `EmployeeCapabilityService` — assign/remove/getByEmployee/getByService/effectiveDuration/effectivePrice
5. `AvailabilityService` güncelle — `employeeId?` parametresi + capability filtresi
6. Controller endpoint'leri:
   - `POST /api/v1/employees/{id}/capabilities`
   - `DELETE /api/v1/employees/{id}/capabilities/{serviceId}`
   - `GET /api/v1/employees/{id}/capabilities`
   - `GET /api/v1/services/{id}/employees`
   - `PUT /api/v1/employees/{id}` (genişletilmiş profil)
   - `GET /api/v1/public/{slug}/services/{serviceId}/employees`
   - `GET /api/v1/public/{slug}/availability?serviceId=&employeeId=&date=`
7. DTO: `EmployeeCapabilityRequest/Response`, `EmployeeDetailResponse`
8. Validasyon: randevu oluştururken capability kontrolü (`AppointmentService`)
9. Unit test: `EmployeeCapabilityServiceTest`, `AvailabilityServiceCapabilityTest`
10. OpenAPI annotation güncelle

**Tahmini süre:** 5-7 gün

---

## CODEX CLI — Görevi

### Platform: OpenAI Codex CLI (terminal)
### Branch: `feature/plan-model`
### Skill'ler: `battle-plan`, `planner`, `test-writer`, `requirements-analyst`
### Kural dosyaları: `AGENTS.md` (kuruldu, 18 skill + backend kuralları)

### Modül 4: Plan Modeli (DB + Limit Enforcement) — WAVE 1

**Yapılacaklar:**

#### Backend
1. `Plan` entity:
   ```java
   // code: STARTER, PRO, BUSINESS, PREMIUM
   // max_employees, max_appointments_monthly, max_services, max_branches
   // features: JSON list
   // monthly_price, yearly_price
   ```
2. `BusinessSubscription` entity:
   ```java
   // business_id (unique), plan_id, status (TRIAL/ACTIVE/EXPIRED/SUSPENDED)
   // trial_end_date, current_period_start/end, manual (boolean)
   ```
3. `PlanLimitUsage` entity (aylık sayaç):
   ```java
   // business_id, year, month, appointments_count, sms_sent, whatsapp_sent
   ```
4. Flyway migration: `V{timestamp}__add_plan_subscription.sql`
5. Plan seed verisi (4 plan):
   - STARTER: 1 çalışan, 150 randevu/ay, 10 hizmet, 499 TL
   - PRO: 5 çalışan, 750 randevu/ay, 30 hizmet, 999 TL
   - BUSINESS: 15 çalışan, 2500 randevu/ay, sınırsız hizmet, 1799 TL
   - PREMIUM: sınırsız, 2999 TL, e-fatura dahil
6. `SubscriptionService` — getCurrentPlan, isFeatureEnabled, checkLimit
7. `PlanLimitInterceptor` (Spring AOP veya HandlerInterceptor):
   - Çalışan oluşturma → max_employees
   - Hizmet oluşturma → max_services
   - Randevu oluşturma → aylık appointments_count
   - Aşımda HTTP 402 + anlamlı Türkçe hata mesajı
8. `BusinessSubscription` — kayıt sırasında otomatik TRIAL başlat (14 gün)
9. API:
   - `GET /api/v1/plans` (public)
   - `GET /api/v1/me/subscription`
   - `GET /api/v1/me/subscription/usage`
   - `POST /api/v1/admin/businesses/{id}/plan` (admin)
10. Unit test: limit enforcement, trial logic

**İşletme kaydı sırasında:** `AuthService.register()` içinde STARTER planı ile TRIAL subscription otomatik oluştur.

**Tahmini süre:** 4-5 gün

---

## CURSOR — Görevi

### Platform: Cursor IDE (Agent modu)
### Branch: `feature/frontend-modernization`
### Skill'ler: `frontend-design`, `web-design-guidelines`, `battle-plan`, `debugger`
### Kural dosyaları: `.cursor/rules/` (13 rule, frontend-vue.mdc dahil, kuruldu)

### Modül 5 Başlangıç: Frontend Modernizasyon — WAVE 1

**Yapılacaklar:**

#### Setup
1. Tailwind CSS 4 kurulum: `npm install tailwindcss @tailwindcss/vite`
2. PrimeVue 4 kurulum: `npm install primevue @primevue/themes`
3. Lucide Vue: `npm install lucide-vue-next`
4. vee-validate + zod: `npm install vee-validate zod @vee-validate/zod`
5. vue-sonner: `npm install vue-sonner`
6. ApexCharts: `npm install vue3-apexcharts apexcharts`
7. FullCalendar Vue: `npm install @fullcalendar/vue3 @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/interaction`
8. vue-i18n: `npm install vue-i18n`
9. `vite.config.ts` güncelle (Tailwind plugin)
10. `main.ts` güncelle (PrimeVue, Sonner, i18n kayıt)

#### Tasarım Sistemi (`appointment-frontend/src/assets/`)
11. `tailwind.config.ts` — renk paleti (primary, success, warning, danger, neutral), font (Inter)
12. `src/assets/theme.ts` — PrimeVue tema konfigürasyonu
13. `src/assets/style.css` — global stiller, Inter font import
14. `src/locales/tr.json` — Türkçe çeviriler (temel anahtarlar)
15. `src/locales/en.json` — İngilizce

#### Temel Bileşenler (`appointment-frontend/src/components/ui/`)
16. `AppButton.vue` — variant (primary/secondary/danger/ghost), size, loading
17. `AppCard.vue` — başlık, içerik, eylemler slotu
18. `AppModal.vue` — başlık, içerik, footer
19. `AppTable.vue` — columns prop, loading skeleton, boş durum
20. `AppEmptyState.vue` — ikon, başlık, açıklama, eylem butonu
21. `AppBadge.vue` — status badge (PENDING/CONFIRMED/RISKY/vb renk sistemi)
22. `AppToast.vue` — vue-sonner wrapper
23. `AppSkeleton.vue` — yükleme iskeleti

#### Layout
24. `AdminLayout.vue` yenile — sidebar (Tailwind), top bar, breadcrumb
25. Sidebar navigasyon: Dashboard, Takvim, Randevular, Müşteriler, Çalışanlar, Hizmetler, Paketler, Ayarlar

#### Sayfalar (İskelet → Gerçek)
26. `LoginView.vue` — modern form, validasyon
27. `RegisterView.vue` — çok adımlı (işletme bilgisi + kullanıcı)
28. `DashboardView.vue` — KPI kartları (bugün randevu, bu ay tahmini, doluluk %, no-show)

**Tahmini süre:** 5-7 gün

---

## WAVE 2 (Wave 1 bittikten sonra)

### Claude Code → Modül 3 Backend (Public Booking)
- Multi-service appointment (birden fazla hizmet tek randevu)
- WhatsApp OTP doğrulama endpoint'leri
- ICS dosyası üretici
- Token bazlı iptal endpoint'i
- Webhook two-way tamamlama (Geliyorum/İptal/Erteleme button callbacks)
- HMAC X-Hub-Signature-256 doğrulama

### Codex → Test + Migration Tamamlama
- Modül 1 integration testleri (Testcontainers PostgreSQL)
- Modül 4 integration testleri
- Tüm Flyway migration'larını doğrula
- `AppointmentService` randevu limit ve capability kontrollerini test et

### Cursor → Modül 3 Frontend + Modül 4 Frontend
- Modern public booking sayfası `/b/:slug` (3 adımlı, mobil-first)
- Çalışan seçim kartları (foto, unvan, deneyim, sadece capability'si olanlar)
- Tarih/saat seçici (FullCalendar veya özel takvim bileşeni)
- Plan sayfası (4 plan kartı + özellik matrisi)
- Kullanım dashboard widget'ı (`/ayarlar/plan`)

---

## WAVE 3

### Claude Code → Dashboard API (Modül 5 Backend)
- `/api/v1/dashboard/overview`
- `/api/v1/dashboard/revenue-trend`
- `/api/v1/dashboard/employee-occupancy`
- `/api/v1/dashboard/hourly-heatmap`
- `/api/v1/dashboard/no-show-rate`

### Cursor → Dashboard Frontend + Analitik
- ApexCharts grafikler (randevu trendi, çalışan doluluk, hizmet dağılımı, saat ısı haritası)
- FullCalendar tam entegrasyon (sürükle-bırak, çalışan filtresi)
- Onboarding wizard (5 adım)

### Codex → E-Fatura Backend (Modül 6)
- Nilvera API entegrasyonu
- Invoice/InvoiceItem entity + migration
- Otomatik draft fatura (appointment COMPLETED → premium plan)
- `@RequiresPlan(PREMIUM)` annotation

---

## Ortak Kurallar (Tüm Agentlar)

Bunlar hem `CLAUDE.md`, `AGENTS.md` ve `.cursor/rules/` içinde kurulu:

1. **Multi-tenant:** Her sorgu `business_id` filtreli
2. **DTO dön:** Entity controller'dan dönme
3. **Validation:** Bean Validation + service katmanı
4. **Migration:** Entity değişikliği → Flyway script
5. **Test:** Happy path + error case (min)
6. **Conventional commits:** `feat(employee-capability): ...`
7. **Türkçe mesaj, İngilizce kod**
8. **PR checklist:** [ ] Test [ ] Migration [ ] OpenAPI [ ] i18n [ ] Responsive

---

## Dosya Yapısı (Kurulum Sonrası)

```
appointment-service/
├── CLAUDE.md              ← Claude Code kuralları + skill referansları (494 satır)
├── AGENTS.md              ← Codex CLI kuralları + skill referansları (494 satır)
├── .cursor/
│   ├── rules/             ← 13 .mdc kural dosyası (Cursor otomatik okur)
│   └── skills/            ← 21 skill (Cursor agent'ı kullanır)
├── .claude/
│   └── skills/            ← 18 backend skill
└── .agents/
    └── skills/            ← 18 backend skill
```

---

## Agent Başlatma Özeti

| Agent | Araç | Branch | WAVE 1 Görevi |
|---|---|---|---|
| **Claude Code** | `claude` CLI | `feature/employee-capability` | Modül 1 — Çalışan-Hizmet Yetkinliği |
| **Codex CLI** | `codex` CLI | `feature/plan-model` | Modül 4 — Plan Modeli + Limit |
| **Cursor** | Cursor IDE Agent | `feature/frontend-modernization` | Frontend Stack Kurulum + Tasarım Sistemi |

*Detaylı açılış promptları için `AGENT-PROMPTLARI.md` dosyasına bakın.*
