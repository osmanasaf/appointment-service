# Cursor Rules — Package Structure - Appointment Service

## Layered Package Architecture

### Proje Yapısı
Proje **Layered Package Structure** kullanır (Feature-based DEĞİL).
Base package: `com.asaf.appointment_service`

```
com.asaf.appointment_service/
├── entity/
│   ├── business/
│   │   ├── Business.java
│   │   ├── WorkingHours.java
│   │   └── BusinessSettings.java
│   ├── employee/
│   │   └── Employee.java
│   ├── service/
│   │   └── ServiceDefinition.java
│   ├── customer/
│   │   ├── Customer.java
│   │   └── CustomerPackage.java
│   ├── appointment/
│   │   └── Appointment.java
│   └── notification/
│       └── NotificationLog.java
├── dto/
│   ├── business/
│   │   ├── BusinessResponse.java
│   │   ├── CreateBusinessRequest.java
│   │   └── UpdateBusinessRequest.java
│   ├── employee/
│   │   ├── EmployeeResponse.java
│   │   └── CreateEmployeeRequest.java
│   ├── service/
│   │   ├── ServiceResponse.java
│   │   └── CreateServiceRequest.java
│   ├── customer/
│   │   ├── CustomerResponse.java
│   │   ├── CreateCustomerRequest.java
│   │   └── PackageResponse.java
│   ├── appointment/
│   │   ├── AppointmentResponse.java
│   │   ├── CreateAppointmentRequest.java
│   │   ├── AvailableSlotResponse.java
│   │   └── ConfirmAppointmentRequest.java
│   └── notification/
│       └── NotificationResponse.java
├── repository/
│   ├── business/
│   │   ├── BusinessRepository.java
│   │   └── WorkingHoursRepository.java
│   ├── employee/
│   │   └── EmployeeRepository.java
│   ├── service/
│   │   └── ServiceRepository.java
│   ├── customer/
│   │   ├── CustomerRepository.java
│   │   └── CustomerPackageRepository.java
│   ├── appointment/
│   │   └── AppointmentRepository.java
│   └── notification/
│       └── NotificationLogRepository.java
├── service/
│   ├── business/
│   │   └── BusinessService.java
│   ├── employee/
│   │   └── EmployeeService.java
│   ├── service/
│   │   └── ServiceDefinitionService.java
│   ├── customer/
│   │   ├── CustomerService.java
│   │   └── PackageService.java
│   ├── appointment/
│   │   ├── AppointmentService.java
│   │   └── AvailabilityService.java
│   └── notification/
│       ├── NotificationService.java
│       └── WhatsAppService.java
├── controller/
│   ├── business/
│   │   └── BusinessController.java
│   ├── employee/
│   │   └── EmployeeController.java
│   ├── service/
│   │   └── ServiceController.java
│   ├── customer/
│   │   └── CustomerController.java
│   ├── appointment/
│   │   ├── AppointmentController.java
│   │   └── PublicAppointmentController.java
│   ├── notification/
│   │   └── WebhookController.java
│   └── auth/
│       └── AuthController.java
├── mapper/
│   ├── business/
│   │   └── BusinessMapper.java
│   ├── employee/
│   │   └── EmployeeMapper.java
│   ├── service/
│   │   └── ServiceMapper.java
│   ├── customer/
│   │   └── CustomerMapper.java
│   └── appointment/
│       └── AppointmentMapper.java
├── exception/
│   ├── business/
│   │   └── BusinessNotFoundException.java
│   ├── employee/
│   │   └── EmployeeNotFoundException.java
│   ├── customer/
│   │   └── CustomerNotFoundException.java
│   ├── appointment/
│   │   ├── AppointmentNotFoundException.java
│   │   ├── SlotNotAvailableException.java
│   │   └── InvalidAppointmentStateException.java
│   └── package/
│       └── InsufficientSessionsException.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── domain/
│   ├── valueobject/
│   │   ├── TimeSlot.java
│   │   ├── PhoneNumber.java
│   │   ├── ServiceDuration.java
│   │   └── Money.java
│   ├── service/
│   │   ├── AvailabilityCalculator.java
│   │   └── AppointmentSchedulingService.java
│   └── event/
│       ├── AppointmentCreated.java
│       ├── AppointmentConfirmed.java
│       └── AppointmentCancelled.java
├── integration/
│   ├── whatsapp/
│   │   ├── WhatsAppClient.java
│   │   ├── WhatsAppMessageTemplate.java
│   │   └── WhatsAppWebhookHandler.java
│   └── sms/
│       └── SmsClient.java
└── common/
    ├── entity/
    │   └── BaseEntity.java
    ├── dto/
    │   ├── ApiResponse.java
    │   └── PageResponse.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── BusinessException.java
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── JpaConfig.java
    │   └── WebConfig.java
    ├── statemachine/
    │   ├── StateMachine.java
    │   └── StateTransition.java
    └── service/
        ├── BaseService.java
        └── BaseController.java
```

---

## Katman Kuralları

### Entity Layer
- Domain entities
- JPA annotations kullanılabilir (pratik yaklaşım)
- Domain factory methods
- Business logic encapsulation
- **Her entity BaseEntity'den extend eder**

### Domain Layer (YENİ)
- Value Objects (immutable)
- Domain Services
- Domain Events
- Framework bağımsız (Spring, JPA yok)

### DTO Layer
- Request/Response objects
- Validation annotations (@NotNull, @Size, vb.)
- Feature bazlı alt paketler
- Record class tercih edilir

### Repository Layer
- Spring Data JPA repositories
- Custom query methods
- Feature bazlı alt paketler
- **Specification pattern desteklenir**

### Service Layer
- Business logic orchestration
- Transaction management
- **BaseService'den extend edilir**
- Feature bazlı alt paketler

### Controller Layer
- REST endpoints
- Request validation
- **BaseController'dan extend edilebilir**
- Feature bazlı alt paketler

### Mapper Layer
- MapStruct interfaces
- Entity ↔ DTO conversion
- Feature bazlı alt paketler

### Integration Layer (YENİ)
- External API clients (WhatsApp, SMS)
- Webhook handlers
- Third-party service adapters

---

## Import Kuralları

```
Controller → Service → Repository → Entity
     ↓           ↓           ↓
    DTO   →   Mapper   →   Domain
```

- **DTO'lar** her katmanda kullanılabilir
- **Entity'ler** sadece repository ve service katmanında
- **Domain objeler** service ve entity katmanında
- **Circular dependency YASAK**

---

## Modül Bazlı Dosya Listesi

### Business Modülü
```
entity/business/
├── Business.java
├── WorkingHours.java
└── BusinessSettings.java

dto/business/
├── BusinessResponse.java
├── CreateBusinessRequest.java
├── UpdateBusinessRequest.java
└── WorkingHoursRequest.java

repository/business/
└── BusinessRepository.java

service/business/
└── BusinessService.java

controller/business/
└── BusinessController.java

mapper/business/
└── BusinessMapper.java

exception/business/
└── BusinessNotFoundException.java
```

### Appointment Modülü
```
entity/appointment/
└── Appointment.java

dto/appointment/
├── AppointmentResponse.java
├── CreateAppointmentRequest.java
├── AvailableSlotResponse.java
├── ConfirmAppointmentRequest.java
└── RiskyAppointmentResponse.java

repository/appointment/
└── AppointmentRepository.java

service/appointment/
├── AppointmentService.java
└── AvailabilityService.java

controller/appointment/
├── AppointmentController.java (işletme için)
└── PublicAppointmentController.java (müşteri için)

mapper/appointment/
└── AppointmentMapper.java

exception/appointment/
├── AppointmentNotFoundException.java
├── SlotNotAvailableException.java
└── InvalidAppointmentStateException.java
```

---

## Yeni Modül Ekleme Kontrol Listesi

Yeni bir modül (örn: `waitlist`) eklenirken:

1. `entity/waitlist/` - Entity oluştur
2. `dto/waitlist/` - Request/Response DTO'ları oluştur
3. `repository/waitlist/` - Repository interface oluştur
4. `service/waitlist/` - Service sınıfı oluştur (BaseService'den extend)
5. `controller/waitlist/` - Controller oluştur
6. `mapper/waitlist/` - MapStruct mapper oluştur
7. `exception/waitlist/` - Custom exception'lar oluştur
8. **Test dosyalarını** ilgili yapıda oluştur
