# Cursor Rules — Clean Code & SOLID Principles - Appointment Service

## Comments Policy

### ❌ NO Inline Comments
- **Absolutely NO** `//` or `/* */` comments in code
- Code must be **self-documenting** through clear naming
- If you need a comment, refactor the code instead

### ✅ JavaDoc Allowed
- JavaDoc is permitted for **public API documentation**
- Use for classes, public methods, and interfaces
- Keep JavaDoc concise and meaningful

---

## DRY (Don't Repeat Yourself)

### Use Base Abstractions
- **BaseService**: All services MUST extend `BaseService<T, ID, R>` for common CRUD operations
- **BusinessAwareService**: Services for business-scoped entities MUST extend `BusinessAwareService<T, ID, R>`
- **BaseController**: Controllers SHOULD extend `BaseController<REQUEST, RESPONSE, ID>` when applicable

#### BusinessAwareService Örneği (DRY için ZORUNLU)
```java
@Service
public class EmployeeService extends BusinessAwareService<Employee, Long, EmployeeRepository> {
    
    public EmployeeService(EmployeeRepository repository, BusinessService businessService) {
        super(repository, "Çalışan", businessService);
    }
    
    @Transactional
    public Employee createEmployee(CreateEmployeeRequest request) {
        validateBusinessExists(request.businessId());  // ✅ Tekrar yok
        Employee employee = EmployeeMapper.toEntity(request);
        return repository.save(employee);
    }
}
```

### Entity Phone Number Pattern (ZORUNLU)
- All entities with phone MUST implement `getFormattedPhoneNumber()` method
- **NEVER** concatenate phone in mapper or controller

```java
// ✅ Doğru: Entity'de method
public String getFormattedPhoneNumber() {
    if (phoneNumber == null) return null;
    String code = phoneCountryCode != null ? phoneCountryCode : "+90";
    return code + phoneNumber;
}

// Mapper'da kullanım
entity.getFormattedPhoneNumber()  // ✅ Doğru

// ❌ Yanlış: Mapper'da concatenation
entity.getPhoneCountryCode() + entity.getPhoneNumber()  // YASAK!
```

### Mapper List Pattern (ZORUNLU)
- Each mapper MUST have both `toResponse()` and `toResponseList()` methods
- Controllers MUST use `toResponseList()` for list endpoints

```java
public class CustomerMapper {
    
    public static CustomerResponse toResponse(Customer entity) { ... }
    
    public static List<CustomerResponse> toResponseList(List<Customer> entities) {
        return entities.stream().map(CustomerMapper::toResponse).toList();
    }
}

// Controller'da kullanım
CustomerMapper.toResponseList(customers)  // ✅ Doğru

// ❌ Yanlış: Controller'da inline stream
customers.stream().map(CustomerMapper::toResponse).toList()  // YASAK!
```

### Utility Classes
- **SlugGenerator**: Use for URL-friendly slug generation
- Complex logic MUST be extracted to utility classes (SRP)

```java
@Component
public class SlugGenerator {
    public String generate(String input, Function<String, Boolean> existsCheck) { ... }
}

// Service'te kullanım
slugGenerator.generate(request.name(), repository::existsBySlug)
```

### Avoid Code Duplication
- Extract common logic into utility methods
- Use inheritance and composition
- Apply design patterns when appropriate

---

## KISS (Keep It Simple, Stupid)

- Prefer simple solutions over complex ones
- Avoid premature optimization
- Write code that is easy to understand and maintain
- **MVP First**: İlk versiyonda basit çözümler, sonra optimize

### Null-Safe Defaults (ZORUNLU)
- Use `Objects.requireNonNullElse()` for default values
- Avoid long ternary chains

```java
// ✅ Doğru: Objects.requireNonNullElse
business.updateProfile(
    Objects.requireNonNullElse(request.name(), business.getName()),
    Objects.requireNonNullElse(request.email(), business.getEmail()),
    Objects.requireNonNullElse(request.address(), business.getAddress())
);

// ❌ Yanlış: Ternary chain
business.updateProfile(
    request.name() != null ? request.name() : business.getName(),
    request.email() != null ? request.email() : business.getEmail(),
    request.address() != null ? request.address() : business.getAddress()
);
```

---

## OOP Principles

### Encapsulation
- Use domain methods instead of setters
- Keep business logic in domain entities

```java
// ✅ Doğru: Domain method
appointment.confirm();
appointment.markAsRisky("Onay alınamadı");
customerPackage.deductSession();

// ❌ Yanlış: Direct setter
appointment.setStatus(AppointmentStatus.CONFIRMED);
appointment.setRisky(true);
```

### Single Responsibility
- Each class should have one reason to change
- Separate concerns (entity, service, controller, mapper)

---

## Design Patterns (MANDATORY)

### Template Method Pattern
- **BaseService** and **BaseController** use template method pattern
- Override abstract methods for custom behavior
- Reuse common operations

```java
public abstract class BaseService<T, ID, R extends JpaRepository<T, ID>> {
    
    protected final R repository;
    protected final String entityName;
    
    public T findByIdOrThrow(ID id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(entityName + " not found: " + id));
    }
    
    public T save(T entity) {
        return repository.save(entity);
    }
    
    public void deleteById(ID id) {
        findByIdOrThrow(id);
        repository.deleteById(id);
    }
}
```

### State Machine Pattern
- Use `StateMachine<S>` for managing appointment states
- Implement `StateTransition` interface for validation

```java
public enum AppointmentStatus {
    PENDING,      // Beklemede
    CONFIRMED,    // Onaylandı
    RISKY,        // Riskli (onaylanmadı)
    CANCELLED,    // İptal edildi
    COMPLETED,    // Tamamlandı
    NO_SHOW       // Gelmedi
}

public class AppointmentStateMachine extends StateMachine<AppointmentStatus> {
    
    @Override
    protected void defineTransitions() {
        addTransition(PENDING, CONFIRMED);
        addTransition(PENDING, CANCELLED);
        addTransition(PENDING, RISKY);
        addTransition(CONFIRMED, CANCELLED);
        addTransition(CONFIRMED, COMPLETED);
        addTransition(CONFIRMED, NO_SHOW);
        addTransition(RISKY, CONFIRMED);
        addTransition(RISKY, CANCELLED);
    }
}
```

### Strategy Pattern
- Use for flexible availability calculation
- Different strategies for different service types

```java
public interface AvailabilityStrategy {
    List<TimeSlot> calculateAvailableSlots(
        LocalDate date,
        Employee employee,
        ServiceDefinition service
    );
}

public class StandardAvailabilityStrategy implements AvailabilityStrategy {
    // Normal hizmetler için
}

public class SameDayAvailabilityStrategy implements AvailabilityStrategy {
    // Aynı gün randevular için (daha kısıtlı saatler)
}
```

### Factory Pattern
- Use for creating notifications

```java
public class NotificationFactory {
    
    public Notification createAppointmentReminder(Appointment appointment) {
        return new WhatsAppNotification(
            appointment.getCustomer().getPhoneNumber(),
            buildReminderMessage(appointment)
        );
    }
    
    public Notification createConfirmationRequest(Appointment appointment) {
        return new WhatsAppNotification(
            appointment.getCustomer().getPhoneNumber(),
            buildConfirmationMessage(appointment)
        );
    }
}
```

---

## Naming Conventions

### Class Naming
- **Classes**: PascalCase (e.g., `AppointmentService`)
- **Interfaces**: PascalCase, adjective or noun (e.g., `AvailabilityStrategy`)
- **Enums**: PascalCase (e.g., `AppointmentStatus`)

### Method Naming
- **Methods**: camelCase (e.g., `confirmAppointment`)
- **Boolean methods**: `is`, `has`, `can` prefix
- **Factory methods**: `create`, `of`, `from` prefix
- **Query methods**: `find`, `get`, `calculate` prefix

```java
// Query methods
findAppointmentsByDate()
getAvailableSlots()
calculateRiskScore()

// Action methods
confirmAppointment()
cancelAppointment()
sendReminder()

// Boolean methods
isSlotAvailable()
hasActivePackage()
canScheduleSameDay()
```

### Variable Naming
- **Variables**: camelCase (e.g., `appointmentId`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_ADVANCE_BOOKING_DAYS`)
- **Packages**: lowercase (e.g., `com.asaf.appointment_service.service.appointment`)

---

## Method Rules

### Size Limits
- Keep methods small (**max 20-30 lines**)
- Extract complex logic into private methods
- One level of abstraction per method

### Single Responsibility
- Each method should do ONE thing
- Side effects should be explicit

```java
// ✅ Doğru: Her method tek sorumluluk
public Appointment confirmAppointment(Long appointmentId) {
    Appointment appointment = findByIdOrThrow(appointmentId);
    appointment.confirm();
    Appointment saved = repository.save(appointment);
    eventPublisher.publish(new AppointmentConfirmed(appointmentId));
    return saved;
}

// ❌ Yanlış: Çoklu sorumluluk
public Appointment processAppointment(Long id, String action, CustomerData data, ...) {
    // 100+ satır kod
}
```

---

## Null Handling

- **Null yerine Optional veya empty list** kullan
- **@Nullable** veya **@NonNull** annotation kullan
- Early return pattern uygula

```java
// ✅ Doğru
public Optional<Appointment> findNextAppointment(Long customerId) {
    return repository.findFirstByCustomerIdAndStatusOrderByScheduledAtAsc(
        customerId, AppointmentStatus.CONFIRMED
    );
}

// ✅ Doğru: Empty list
public List<TimeSlot> getAvailableSlots() {
    // Slot yoksa boş liste döner
    return slots.isEmpty() ? Collections.emptyList() : slots;
}

// ❌ Yanlış
public Appointment findNextAppointment(Long customerId) {
    return null; // ASLA null dönme
}
```

---

## SOLID Principles

### S – Single Responsibility
Her sınıfın sadece bir değişim nedeni olmalı.

```java
// AppointmentService: Randevu iş mantığı
// AvailabilityService: Müsaitlik hesaplama
// NotificationService: Bildirim gönderme
// WhatsAppClient: WhatsApp API entegrasyonu
```

### O – Open/Closed
Yeni davranışlar eklenebilmeli, mevcut kod değiştirilmemeli.

```java
// Strategy pattern ile yeni availability hesaplama eklenebilir
// Factory pattern ile yeni notification türleri eklenebilir
```

### L – Liskov Substitution
Alt sınıf, üst sınıf yerine geçebilmeli.

### I – Interface Segregation
Büyük interface'ler küçük parçalara bölünmeli.

```java
// ❌ Yanlış: Fat interface
public interface AppointmentOperations {
    void create();
    void confirm();
    void cancel();
    void calculateSlots();
    void sendNotification();
}

// ✅ Doğru: Ayrılmış interface'ler
public interface AppointmentLifecycle { ... }
public interface AvailabilityCalculator { ... }
public interface NotificationSender { ... }
```

### D – Dependency Inversion
High-level modüller abstraction'a bağlı olmalı.

```java
// Service, interface'e bağlı
public class NotificationService {
    private final MessageSender messageSender; // Interface
    
    public NotificationService(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
}

// WhatsAppSender implements MessageSender
// SmsSender implements MessageSender
```

---

## Exception Handling

### Business vs Technical Exceptions
```java
// Business Exception
public class SlotNotAvailableException extends BusinessException {
    public SlotNotAvailableException(TimeSlot slot) {
        super("Seçilen saat dilimi müsait değil: " + slot);
    }
}

// Technical Exception → GlobalExceptionHandler'da yakalanır
```

### Exception Hierarchy
```
BusinessException (abstract)
├── EntityNotFoundException
│   ├── AppointmentNotFoundException
│   ├── CustomerNotFoundException
│   └── BusinessNotFoundException
├── ValidationException
│   ├── SlotNotAvailableException
│   └── InvalidAppointmentStateException
└── InsufficientResourceException
    └── InsufficientSessionsException
```

---

## Logging

```java
// INFO: Ana akış
log.info("Randevu oluşturuldu: appointmentId={}", appointmentId);

// WARN: Beklenen anormallikler
log.warn("Müşteri randevuyu onaylamadı: appointmentId={}", appointmentId);

// ERROR: Gerçek hatalar
log.error("WhatsApp mesajı gönderilemedi: customerId={}", customerId, exception);

// DEBUG: Geliştirme detayları (production'da kapalı)
log.debug("Müsait slotlar hesaplandı: count={}", slots.size());
```

---

## Testability

- **Constructor injection** kullan
- **Static utility** az kullan
- **Interface** üzerinden bağımlılık
- **Pure functions** tercih et

```java
// ✅ Test edilebilir
@Service
public class AppointmentService {
    private final AppointmentRepository repository;
    private final AvailabilityCalculator calculator;
    private final NotificationService notificationService;
    
    public AppointmentService(
        AppointmentRepository repository,
        AvailabilityCalculator calculator,
        NotificationService notificationService
    ) {
        this.repository = repository;
        this.calculator = calculator;
        this.notificationService = notificationService;
    }
}
```
