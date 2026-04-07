# Modül 07: Frontend Öneriler (Referans)

## Amaç
Frontend'i ayrı proje/repo olarak planlamak; backend API sözleşmesini ve güvenlik notlarını netleştirmek. Bu modülde sadece planlama dokümanı yer alır, kod bu repo'da yazılmaz.

## Kabul kriterleri
- [ ] Hedef stack belirtilir (React veya Vue; mobil uyumlu web).
- [ ] Kullanılacak API'ler listelenir: public randevu (slug ile işletme, slotlar, randevu oluşturma), auth (login, register, JWT), işletme paneli (business, employee, service, customer, appointment CRUD).
- [ ] CORS ve güvenlik notları: Backend'de CORS konfigürasyonu gerekebilir; JWT Authorization header kullanımı.

## Etkilenen / yeni dosyalar
- Sadece bu .md dosyası. Backend tarafında ileride CORS ayarı eklenebilir (WebConfig veya SecurityConfig).

## Teknik notlar
- Backend: Spring Boot; REST API; public prefix `/api/v1/public`, authenticated prefix `/api/v1` (business, appointments vb.).
- Auth: POST /api/v1/auth/register, POST /api/v1/auth/login → token; Authorization: Bearer <token>.
- Public: GET /api/v1/public/business/{slug}, GET slots, POST create appointment (slug veya businessId ile).

## Alt görevler
- [ ] (Frontend projesi ayrı) React/Vue kurulumu
- [ ] (Frontend) Public randevu sayfası: işletme bilgisi, hizmet/çalışan seçimi, slot seçimi, form submit
- [ ] (Frontend) İşletme paneli: giriş, takvim, randevu listesi, müşteri/paket yönetimi
- [ ] (Backend) Gerekirse CORS allow origin konfigürasyonu
