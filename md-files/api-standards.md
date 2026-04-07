# Cursor Rules — API Standards - Appointment Service

## RESTful API Design

### URL Conventions

```
Base URL: /api/v1

# Business (İşletme Yönetimi)
GET    /api/v1/businesses                    # Liste
POST   /api/v1/businesses                    # Oluştur
GET    /api/v1/businesses/{id}               # Detay
PUT    /api/v1/businesses/{id}               # Güncelle
DELETE /api/v1/businesses/{id}               # Sil
GET    /api/v1/businesses/{id}/employees     # İşletmenin çalışanları
GET    /api/v1/businesses/{id}/services      # İşletmenin hizmetleri
GET    /api/v1/businesses/{id}/appointments  # İşletmenin randevuları

# Employee (Çalışan)
GET    /api/v1/employees                     # Liste
POST   /api/v1/employees                     # Oluştur
GET    /api/v1/employees/{id}                # Detay
PUT    /api/v1/employees/{id}                # Güncelle
DELETE /api/v1/employees/{id}                # Sil
GET    /api/v1/employees/{id}/appointments   # Çalışanın randevuları
GET    /api/v1/employees/{id}/availability   # Çalışanın müsaitliği

# Service (Hizmet)
GET    /api/v1/services                      # Liste
POST   /api/v1/services                      # Oluştur
GET    /api/v1/services/{id}                 # Detay
PUT    /api/v1/services/{id}                 # Güncelle
DELETE /api/v1/services/{id}                 # Sil

# Customer (Müşteri)
GET    /api/v1/customers                     # Liste
POST   /api/v1/customers                     # Oluştur
GET    /api/v1/customers/{id}                # Detay
PUT    /api/v1/customers/{id}                # Güncelle
GET    /api/v1/customers/{id}/packages       # Müşterinin paketleri
GET    /api/v1/customers/{id}/appointments   # Müşterinin randevuları

# Appointment (Randevu)
GET    /api/v1/appointments                  # Liste (filtrelenebilir)
POST   /api/v1/appointments                  # Oluştur
GET    /api/v1/appointments/{id}             # Detay
PUT    /api/v1/appointments/{id}             # Güncelle
DELETE /api/v1/appointments/{id}             # İptal (soft delete)
POST   /api/v1/appointments/{id}/confirm     # Onayla
POST   /api/v1/appointments/{id}/cancel      # İptal et
POST   /api/v1/appointments/{id}/complete    # Tamamla
POST   /api/v1/appointments/{id}/no-show     # Gelmedi işaretle
GET    /api/v1/appointments/risky            # Riskli randevular

# Availability (Müsaitlik)
GET    /api/v1/availability                  # Müsait slotlar
# Query params: employeeId, serviceId, date, startDate, endDate

# Package (Paket)
GET    /api/v1/packages                      # Liste
POST   /api/v1/packages                      # Oluştur
GET    /api/v1/packages/{id}                 # Detay
POST   /api/v1/packages/{id}/deduct          # Seans düş

# Public Endpoints (Müşteri için - Auth gerektirmez)
GET    /api/v1/public/businesses/{slug}      # İşletme bilgisi
GET    /api/v1/public/businesses/{slug}/services  # Hizmetler
GET    /api/v1/public/availability           # Müsait slotlar
POST   /api/v1/public/appointments           # Randevu talebi

# Webhook Endpoints
POST   /api/v1/webhooks/whatsapp             # WhatsApp callback
```

---

## HTTP Status Codes

```
2xx Success
├── 200 OK              # GET, PUT başarılı
├── 201 Created         # POST başarılı
└── 204 No Content      # DELETE başarılı

4xx Client Error
├── 400 Bad Request     # Validation hatası
├── 401 Unauthorized    # Auth yok
├── 403 Forbidden       # Yetki yok
├── 404 Not Found       # Kaynak bulunamadı
├── 409 Conflict        # State conflict (örn: slot dolu)
└── 422 Unprocessable   # Business rule ihlali

5xx Server Error
├── 500 Internal Error  # Beklenmeyen hata
└── 503 Service Unavailable  # WhatsApp API down vb.
```

---

## Response Formats

### Success Response
```json
{
    "success": true,
    "data": {
        "id": 1,
        "businessName": "Psikoloji Kliniği",
        "createdAt": "2025-01-05T10:00:00Z"
    },
    "message": null
}
```

### List Response (Paginated)
```json
{
    "success": true,
    "data": {
        "content": [...],
        "page": 0,
        "size": 20,
        "totalElements": 100,
        "totalPages": 5,
        "hasNext": true,
        "hasPrevious": false
    },
    "message": null
}
```

### Error Response
```json
{
    "success": false,
    "data": null,
    "error": {
        "code": "SLOT_NOT_AVAILABLE",
        "message": "Seçilen saat dilimi müsait değil",
        "details": {
            "requestedSlot": "2025-01-10T14:00:00",
            "conflictingAppointmentId": 42
        },
        "timestamp": "2025-01-05T10:00:00Z"
    }
}
```

### Validation Error Response
```json
{
    "success": false,
    "data": null,
    "error": {
        "code": "VALIDATION_ERROR",
        "message": "Girilen veriler geçersiz",
        "fieldErrors": [
            {
                "field": "phoneNumber",
                "message": "Telefon numarası geçerli formatta olmalı"
            },
            {
                "field": "scheduledAt",
                "message": "Randevu tarihi gelecekte olmalı"
            }
        ],
        "timestamp": "2025-01-05T10:00:00Z"
    }
}
```

---

## Request DTOs

### Naming Convention
- `Create{Entity}Request` - POST için
- `Update{Entity}Request` - PUT için
- `{Entity}Response` - GET için
- `{Entity}ListResponse` - Liste için

### Validation Annotations
```java
public record CreateAppointmentRequest(
    @NotNull(message = "Müşteri ID zorunlu")
    Long customerId,
    
    @NotNull(message = "Hizmet ID zorunlu")
    Long serviceId,
    
    @NotNull(message = "Çalışan ID zorunlu")
    Long employeeId,
    
    @NotNull(message = "Randevu tarihi zorunlu")
    @Future(message = "Randevu tarihi gelecekte olmalı")
    LocalDateTime scheduledAt,
    
    @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
    String notes
) {}
```

---

## API Response Wrapper

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorDetails error,
    String message
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, null, message);
    }
    
    public static <T> ApiResponse<T> error(ErrorDetails error) {
        return new ApiResponse<>(false, null, error, null);
    }
}
```

---

## Filtering & Pagination

### Query Parameters
```
# Pagination
?page=0&size=20&sort=createdAt,desc

# Filtering
?status=CONFIRMED
?startDate=2025-01-01&endDate=2025-01-31
?employeeId=5
?customerId=10

# Complex filtering
?status=CONFIRMED,PENDING
?risky=true
```

### Controller Implementation
```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getAppointments(
    @RequestParam(required = false) AppointmentStatus status,
    @RequestParam(required = false) LocalDate startDate,
    @RequestParam(required = false) LocalDate endDate,
    @RequestParam(required = false) Long employeeId,
    @RequestParam(required = false) Boolean risky,
    @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.ASC) 
    Pageable pageable
) {
    Page<Appointment> appointments = appointmentService.findFiltered(
        status, startDate, endDate, employeeId, risky, pageable
    );
    return ResponseEntity.ok(ApiResponse.success(
        PageResponse.from(appointments, mapper::toResponse)
    ));
}
```

---

## Date/Time Handling

### ISO 8601 Format
```json
{
    "scheduledAt": "2025-01-10T14:00:00+03:00",
    "createdAt": "2025-01-05T10:30:00Z"
}
```

### Jackson Configuration
```java
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

---

## Versioning Strategy

- URL versioning: `/api/v1/`
- Major version changes → new URL path
- Minor changes → backward compatible

```java
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController { }
```

---

## Rate Limiting (Future)

```
# Public endpoints
/api/v1/public/*     → 60 requests/minute per IP

# Authenticated endpoints
/api/v1/*            → 300 requests/minute per user

# WhatsApp webhooks
/api/v1/webhooks/*   → No limit (IP whitelist)
```

---

## API Documentation

### OpenAPI/Swagger
```java
@Operation(summary = "Randevu oluştur", description = "Yeni bir randevu talebi oluşturur")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Randevu oluşturuldu"),
    @ApiResponse(responseCode = "409", description = "Saat dilimi müsait değil"),
    @ApiResponse(responseCode = "400", description = "Validation hatası")
})
@PostMapping
public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
    @Valid @RequestBody CreateAppointmentRequest request
) { }
```
