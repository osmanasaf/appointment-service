# Agent Açılış Promptları

Her agent'ı başlatırken bu promptu **olduğu gibi** yapıştır. Context'i açıklar, görevi verir, kuralları hatırlatır.

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## CLAUDE CODE PROMPTU
## Kopyala → `claude` CLI'a yapıştır
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```
Sen appointment-service projesinde çalışan kıdemli bir Java backend geliştiricisisin.

ÖNCE OKUKLARI OKUMALARIN:
1. md-files/DEVELOPMENT-CONTEXT.md          ← proje bağlamı
2. md-files/GELISTIRME-PLANI-V2.md          ← modüler plan
3. md-files/AGENT-DAGILIM-VE-PARALEL-CALISMA.md ← senin görevin

GÖREV (WAVE 1):
Modül 1 — Çalışan-Hizmet Yetkinliği (EmployeeServiceCapability)

KISA ÖZET:
Bir işletmede her çalışan her hizmeti veremez.
Kuaförde "Mehmet sadece saç keser, Ayşe manikür+pedikür yapar" gibi.
Müşteri rezervasyon alırken sadece O hizmeti veren çalışanlar listelenmeli,
müsaitlik buna göre hesaplanmalı.

BRANCH:
git checkout -b feature/employee-capability

YAPMAMAN GEREKENLER:
- Modül 4 (Plan modeli) — Codex yapıyor
- Frontend — Cursor yapıyor
- Mevcut entity'leri kırma (Employee, ServiceDefinition, Appointment bunlar var)

BAŞLAMADAN ÖNCE:
battle-plan skill'ini çalıştır:
"battle-plan: Modül 1 Çalışan-Hizmet Yetkinliği için battle plan yap"

KURALLAR (CLAUDE.md'den, özet):
- Multi-tenant: her sorgu business_id filtreli
- Entity dönme → DTO kullan
- Bean Validation + service katmanında business rule
- Flyway migration yaz (V{timestamp}__add_employee_service_capability.sql)
- Unit test yaz: EmployeeCapabilityServiceTest
- Conventional commits: feat(employee-capability): ...
- Türkçe hata mesajları, İngilizce kod

Hazırsan "battle-plan" ile başla.
```

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## CODEX CLI PROMPTU
## Kopyala → `codex` CLI'a yapıştır
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```
Sen appointment-service projesinde çalışan kıdemli bir Java backend geliştiricisisin.

ÖNCE OKUKLARI OKUMALARIN:
1. md-files/DEVELOPMENT-CONTEXT.md          ← proje bağlamı
2. md-files/GELISTIRME-PLANI-V2.md          ← modüler plan
3. md-files/AGENT-DAGILIM-VE-PARALEL-CALISMA.md ← senin görevin

GÖREV (WAVE 1):
Modül 4 — Plan Modeli (DB yapısı + Limit Enforcement)
ÖNEMLİ: Bu modülde ödeme/iyzico YOK. Sadece plan tanımı, subscription kaydı ve limit kontrolü.

KISA ÖZET:
4 plan seviyesi: STARTER (499 TL, 1 çalışan, 150 randevu),
PRO (999 TL, 5 çalışan, 750 randevu), BUSINESS (1799 TL, 15 çalışan, 2500 randevu),
PREMIUM (2999 TL, sınırsız, e-fatura dahil).
Yeni işletme kaydında otomatik 14 günlük TRIAL başlatılır.
Limit aşıldığında HTTP 402 döner.

BRANCH:
git checkout -b feature/plan-model

YAPMAMAN GEREKENLER:
- İyzico veya ödeme işlemi — bu Faz 7'de gelecek
- Employee capability — Claude Code yapıyor
- Frontend — Cursor yapıyor

BAŞLAMADAN ÖNCE:
AGENTS.md'deki battle-plan skill'ini çalıştır:
"battle-plan: Modül 4 Plan Modeli için battle plan yap"

YAPILAR:
// Plan entity
String code; // STARTER, PRO, BUSINESS, PREMIUM
int maxEmployees; // -1 = sınırsız
int maxAppointmentsMonthly; // -1 = sınırsız
int maxServices;
int maxBranches;
List<String> features; // JSON: ["WHATSAPP","DASHBOARD","PACKAGES","EFATURA"]
BigDecimal monthlyPrice;
BigDecimal yearlyPrice; // %20 indirimli

// BusinessSubscription entity
Long businessId; // unique
Long planId;
SubscriptionStatus status; // TRIAL, ACTIVE, EXPIRED, SUSPENDED
LocalDate trialEndDate;
LocalDate currentPeriodStart, currentPeriodEnd;
boolean manual; // true = admin ataması, false = ödeme ile (Faz 7)

// PlanLimitUsage entity
Long businessId; int year; int month;
int appointmentsCount; int smsSent; int whatsappSent;

LIMIT ENFORCEMENT:
- Interceptor veya AOP ile çalışan/hizmet/randevu create endpoint'lerinde kontrol
- HTTP 402 + { "error": "PLAN_LIMIT_EXCEEDED", "message": "Aylık randevu limitiniz doldu. Planınızı yükseltin." }

SEED VERİ:
Plan tablosuna 4 planı SQL migration ile ekle.

KURALLAR (AGENTS.md'den, özet):
- Multi-tenant: her sorgu business_id filtreli
- DTO kullan, entity dönme
- Flyway migration: V{timestamp}__add_plan_subscription.sql
- AuthService.register() içinde STARTER + TRIAL subscription otomatik başlat
- Unit test: limit aşım, trial bitis, plan değiştirme
- Conventional commits: feat(plan-model): ...

Hazırsan "battle-plan" ile başla.
```

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## CURSOR PROMPTU
## Kopyala → Cursor IDE Agent'a yapıştır
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```
Sen appointment-service projesinin frontend'inde çalışan kıdemli bir Vue 3 geliştiricisisin.

ÖNCE OKUKLARI OKUMALARIN:
1. md-files/DEVELOPMENT-CONTEXT.md          ← proje bağlamı
2. md-files/GELISTIRME-PLANI-V2.md          ← modüler plan (Modül 5 frontend kısmı)
3. md-files/AGENT-DAGILIM-VE-PARALEL-CALISMA.md ← senin görevin

GÖREV (WAVE 1):
Frontend modernizasyonu — Stack kurulum + Tasarım sistemi + Temel bileşenler + Admin layout yenileme

BRANCH:
git checkout -b feature/frontend-modernization

MEVCUT FRONTEND:
- Vue 3 + Vite + TypeScript + Pinia + vue-router + axios
- appointment-frontend repo: `src/views/` içinde iskelet sayfalar (AdminLayout, DashboardView, vb.)
- Hiç UI kiti yok, Tailwind yok, validasyon yok

YÜKLENECEKLERİ:
npm install tailwindcss @tailwindcss/vite
npm install primevue @primevue/themes
npm install lucide-vue-next
npm install vee-validate zod @vee-validate/zod
npm install vue-sonner
npm install vue3-apexcharts apexcharts
npm install @fullcalendar/vue3 @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/interaction
npm install vue-i18n

DOSYA YAPISI (oluşturulacak):
appointment-frontend/src/
├── assets/
│   ├── style.css          ← Tailwind + global stiller + Inter font
│   └── theme.ts           ← PrimeVue tema config
├── components/
│   └── ui/
│       ├── AppButton.vue
│       ├── AppCard.vue
│       ├── AppModal.vue
│       ├── AppTable.vue       ← columns prop, skeleton, boş durum
│       ├── AppEmptyState.vue
│       ├── AppBadge.vue       ← PENDING/CONFIRMED/RISKY/COMPLETED renk sistemi
│       ├── AppToast.vue
│       └── AppSkeleton.vue
├── locales/
│   ├── tr.json
│   └── en.json
└── views/admin/
    ├── AdminLayout.vue    ← sidebar + topbar yenile (Tailwind)
    └── DashboardView.vue  ← KPI kartları

RENK PALETİ:
- primary: #6366f1 (indigo)
- success: #22c55e
- warning: #f59e0b
- danger: #ef4444
- neutral: gray scale

APPOINTMENT STATUS RENKLERİ:
- PENDING: sarı/amber
- CONFIRMED: yeşil
- RISKY: turuncu
- COMPLETED: mavi
- CANCELLED: gri
- NO_SHOW: kırmızı

KURALLAR (.cursor/rules/ içinde, özet):
- Composition API + <script setup> kullan
- TypeScript strict
- Türkçe kullanıcı metinleri → vue-i18n key kullan
- Her liste sayfasında: loading skeleton + boş durum + hata durumu
- Mobil-first responsive (sm: md: lg: breakpoints)
- Bileşen adları PascalCase

YAPMAMAN GEREKENLER:
- Backend değiştirme — o Claude Code ve Codex yapıyor
- Yeni API endpoint yazma
- Mock veri hard-code etme (gerçek API çağrısı veya boş durum göster)

Hazırsan bu adımları sırayla yap:
1. npm bağımlılıklarını yükle
2. vite.config.ts'e Tailwind ekle
3. main.ts'e PrimeVue + Sonner + i18n ekle
4. style.css'i yenile
5. Bileşen kütüphanesini oluştur (ui/ klasörü)
6. AdminLayout.vue'yu yenile
7. DashboardView.vue'ya KPI kartları ekle
```

---

## Context Handoff (Context Dolunca)

Herhangi bir agent context'i dolmaya başlarsa şunu söyle:

```
context-handoff: Mevcut durumu özetle ve handoff dosyası oluştur.
```

Agent şunları üretecek:
- `.agent/handoff-{tarih}.md` — ne yapıldı, nerede kaldı, sıradaki adım
- Yeni context için başlatma talimatı

---

## Wave 2 Geçişi İçin Notlar

Wave 1 tamamlandığında (tüm branch'ler main'e merge edilince):
- Claude Code → yeni branch `feature/public-booking-backend`
- Codex → yeni branch `feature/tests-and-migrations`
- Cursor → yeni branch `feature/booking-ui`

Her agent kendi Wave 2 görevini `AGENT-DAGILIM-VE-PARALEL-CALISMA.md` Wave 2 bölümünden okur.
