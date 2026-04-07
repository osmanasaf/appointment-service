# Telefon ve Manuel (Yerinde) Randevu

Kullanıcılar sıklıkla **telefonla arayıp** veya **yerinde gelip** randevu alıyor. Bu randevuların da sisteme girilmesi ve **online randevularla aynı takvimde senkron** tutulması gerekir.

## Senkronizasyon Prensibi

- **Tek kaynak:** Tüm randevular (online, telefon, yerinde) aynı `appointments` tablosunda ve aynı müsaitlik kurallarına tabidir.
- **Çakışma yok:** Telefon veya manuel giriş yapıldığında sistem yine aynı `validateSlotAvailability` ile kontrol eder; çift randevu oluşmaz.
- **Randevu kaynağı:** Her randevuda `source` alanı ile nereden alındığı saklanır: `ONLINE`, `PHONE`, `WALK_IN`, `OTHER`.

## Personel: Telefon / Yerinde Randevu Girişi

Personel, **tek istekte** müşteri adı + telefon ile randevu kaydeder. Müşteri varsa bulunur, yoksa oluşturulur.

**Endpoint:** `POST /api/v1/appointments/staff`

**Body örneği (telefon):**
```json
{
  "businessId": 1,
  "employeeId": 2,
  "serviceId": 3,
  "scheduledAt": "2025-03-10T14:00:00",
  "customerName": "Ayşe Yılmaz",
  "phoneNumber": "5321234567",
  "phoneCountryCode": "+90",
  "notes": "Telefonla aradı, yarın 14:00 uygun",
  "source": "PHONE"
}
```

**Body örneği (yerinde / manuel):**
```json
{
  "businessId": 1,
  "employeeId": 2,
  "serviceId": 3,
  "scheduledAt": "2025-03-10T15:30:00",
  "customerName": "Mehmet Kaya",
  "phoneNumber": "5339876543",
  "phoneCountryCode": "+90",
  "notes": "",
  "source": "WALK_IN"
}
```

- `source` **zorunlu** ve sadece `PHONE` veya `WALK_IN` olabilir.
- Müşteri `phoneNumber` ile aranır; bulunamazsa `customerName` ve telefon ile yeni müşteri oluşturulur.

## Mevcut Randevu Oluşturma (Müşteri ID ile)

Personel zaten kayıtlı bir müşteriye randevu açacaksa (ID biliyorsa) mevcut endpoint kullanılabilir; isteğe bağlı `source` verilir.

**Endpoint:** `POST /api/v1/appointments`

**Body:** `CreateAppointmentRequest` (businessId, customerId, employeeId, serviceId, scheduledAt, notes, customerNotes, **source**).  
`source` verilmezse `ONLINE` kabul edilir. Telefon/yerinde için `PHONE` veya `WALK_IN` gönderilebilir.

## Yanıtta Randevu Kaynağı

Tüm randevu listeleme/detay yanıtlarında `AppointmentResponse` içinde `source` alanı döner. Böylece takvimde veya raporlarda “telefon”, “yerinde”, “online” ayrımı yapılabilir.

## Özet

| Kaynak       | Nasıl girilir                          | Senkron |
|-------------|----------------------------------------|--------|
| Online      | Müşteri public linkten randevu alır   | Evet   |
| Telefon     | Personel `POST /api/v1/appointments/staff` ile `source: PHONE` | Evet   |
| Yerinde     | Personel `POST /api/v1/appointments/staff` ile `source: WALK_IN` | Evet   |

Böylece hem sistem hem manuel (telefon + yerinde) randevular tek takvimde senkron tutulur.
