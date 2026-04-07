# Cursor Rules – Domain Driven Design (DDD) - Appointment Service

## 1. Genel İlke
Bu projede **Domain Driven Design (DDD)** esas alınacaktır.
Kod, teknik detaylardan önce **iş kurallarını ve domain dilini** yansıtmalıdır.

---

## 2. Ubiquitous Language

### Domain Terimleri (Appointment Service)
| Domain Terimi (TR) | Domain Terimi (EN) | Açıklama |
|-------------------|-------------------|----------|
| İşletme | Business | Hizmet veren işletme (psikolog, diyetisyen, kuaför vb.) |
| Çalışan | Employee | İşletmede çalışan kişi |
| Hizmet/İşlem | Service | Sunulan hizmet (terapi seansı, saç kesimi vb.) |
| Müşteri | Customer | Randevu alan müşteri |
| Randevu | Appointment | Zaman bağlı hizmet talebi |
| Paket | Package | Seans paketi (5, 10, 20 seans) |
| Seans | Session | Tek bir hizmet kullanımı |
| Boş Saat | AvailableSlot | Müsait zaman dilimi |
| Onay | Confirmation | Randevu onayı |
| No-show | NoShow | Gelmeyen müşteri |
| Riskli Randevu | RiskyAppointment | Onaylanmayan randevu |

### Naming Örnekleri
❌ `processData()` → ✅ `confirmAppointment()`
❌ `statusFlag` → ✅ `appointmentStatus`
❌ `item` → ✅ `service` veya `appointment`
❌ `handleEvent()` → ✅ `handleAppointmentConfirmation()`

---

## 3. Katmanlı Yapı

### 3.1 Domain Layer
- Domain layer **framework bağımsızdır**
- Spring, JPA, Lombok, Jackson import edilmez (istisnai Value Object'ler hariç)

İçerir:
- Entity (Appointment, Customer, Business, Employee, Service)
- Value Object (TimeSlot, PhoneNumber, Money, Duration)
- Domain Service (AppointmentSchedulingService, AvailabilityCalculator)
- Enum (AppointmentStatus, ServiceType, PackageType)
- Domain Event (AppointmentConfirmed, AppointmentCancelled)

#### Domain Entity Kuralları
```java
// ✅ Doğru: Domain metotları ile state değişimi
public class Appointment {
    public void confirm() {
        if (this.status != AppointmentStatus.PENDING) {
            throw new IllegalAppointmentStateException("Sadece bekleyen randevular onaylanabilir");
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
    
    public void markAsRisky() {
        this.risky = true;
        this.riskReason = "Onay alınamadı";
    }
}

// ❌ Yanlış: Setter kullanımı
appointment.setStatus(CONFIRMED);
```

---

### 3.2 Application Layer
- Use-case orchestration burada yapılır
- Transaction sınırları burada tanımlanır
- Domain objeleri birleştirir ama iş kuralı içermez

#### Use Case Örnekleri
- `CreateAppointmentUseCase` - Yeni randevu oluşturma
- `ConfirmAppointmentUseCase` - Randevu onaylama
- `CancelAppointmentUseCase` - Randevu iptal
- `CalculateAvailableSlotsUseCase` - Müsait saatleri hesaplama
- `DecrementPackageSessionUseCase` - Paket seansı düşürme
- `SendReminderUseCase` - WhatsApp hatırlatma gönderme

---

### 3.3 Infrastructure Layer
- Database, messaging, external API adaptörleri
- JPA Entity, Repository implementasyonları
- WhatsApp Business API entegrasyonu
- Mapping işlemleri (Entity ↔ Domain)

---

## 4. Aggregate Root Kuralları

### Appointment Service Aggregates
| Aggregate Root | Dahil Entities | 
|---------------|----------------|
| Business | Employee, WorkingHours, ServiceDefinition |
| Customer | CustomerPackage, CustomerSession |
| Appointment | (tek başına, diğer aggregate'lere referans) |

### Kurallar
- Aggregate dışından içeriye doğrudan erişim yok
- Aggregate root üzerinden tüm operasyonlar yapılır
- Cross-aggregate referanslar ID ile yapılır

```java
// ✅ Doğru: ID ile referans
public class Appointment {
    private Long businessId;
    private Long customerId;
    private Long employeeId;
    private Long serviceId;
}

// ❌ Yanlış: Doğrudan referans
public class Appointment {
    private Business business;
    private Customer customer;
}
```

---

## 5. Value Object Kuralları

### Tanımlanacak Value Objects
```java
// TimeSlot - Zaman dilimi
public record TimeSlot(LocalDateTime start, LocalDateTime end) {
    public TimeSlot {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Başlangıç bitiş'ten sonra olamaz");
        }
    }
    
    public Duration duration() {
        return Duration.between(start, end);
    }
    
    public boolean overlapsWith(TimeSlot other) {
        return !this.end.isBefore(other.start) && !other.end.isBefore(this.start);
    }
}

// PhoneNumber - Telefon numarası (WhatsApp için kritik)
public record PhoneNumber(String countryCode, String number) {
    public String formatted() {
        return countryCode + number;
    }
    
    public String whatsAppFormat() {
        return countryCode.replace("+", "") + number;
    }
}

// ServiceDuration - Hizmet süresi
public record ServiceDuration(int minutes) {
    public ServiceDuration {
        if (minutes < 15 || minutes > 360) {
            throw new IllegalArgumentException("Süre 15-360 dakika arasında olmalı");
        }
    }
}
```

---

## 6. Domain Event Kuralları

### Event Türleri
```java
// Appointment Domain Events
public record AppointmentCreated(Long appointmentId, LocalDateTime scheduledAt) {}
public record AppointmentConfirmed(Long appointmentId, LocalDateTime confirmedAt) {}
public record AppointmentCancelled(Long appointmentId, String reason) {}
public record AppointmentMarkedRisky(Long appointmentId, String reason) {}
public record AppointmentCompleted(Long appointmentId) {}

// Package Domain Events
public record SessionDeducted(Long packageId, int remainingSessions) {}
public record PackageExpiring(Long packageId, int remainingSessions) {}
```

---

## 7. Domain Service Kuralları

Domain Service, birden fazla Aggregate'i koordine eden veya Entity'e ait olmayan iş mantığını içerir.

```java
// ✅ Domain Service örneği
public class AvailabilityCalculator {
    public List<TimeSlot> calculateAvailableSlots(
        WorkingHours workingHours,
        List<Appointment> existingAppointments,
        ServiceDuration serviceDuration,
        LocalDate date
    ) {
        // Müsait saatleri hesapla
    }
}

// ✅ Scheduling Domain Service
public class AppointmentSchedulingService {
    public boolean canSchedule(
        TimeSlot requestedSlot,
        List<Appointment> employeeAppointments,
        WorkingHours workingHours
    ) {
        // Randevu alınabilir mi kontrol et
    }
}
```
