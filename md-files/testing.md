# Cursor Rules — Testing Standards - Appointment Service

## Testing Strategy

### Test Pyramid

```
          /\
         /  \
        / E2E \          ← 10% (Browser tests, API flow tests)
       /______\
      /        \
     / Integration\       ← 30% (Service + Repository tests)
    /______________\
   /                \
  /    Unit Tests    \   ← 60% (Domain, Service logic)
 /____________________\
```

---

## Unit Tests

### Test Naming Convention
```
methodName_givenCondition_expectedResult()

// Örnekler:
confirm_whenPendingAppointment_shouldChangeStatusToConfirmed()
calculateAvailableSlots_whenNoExistingAppointments_shouldReturnAllSlots()
deductSession_whenNoSessionsRemaining_shouldThrowException()
```

### Test Structure (AAA Pattern)
```java
@Test
void confirm_whenPendingAppointment_shouldChangeStatusToConfirmed() {
    // Arrange
    Appointment appointment = createPendingAppointment();
    
    // Act
    appointment.confirm();
    
    // Assert
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    assertThat(appointment.getConfirmedAt()).isNotNull();
}
```

---

## Domain Layer Tests

### Entity Tests
```java
class AppointmentTest {

    @Test
    void confirm_whenPendingAppointment_shouldChangeStatusToConfirmed() {
        Appointment appointment = Appointment.builder()
            .status(AppointmentStatus.PENDING)
            .scheduledAt(LocalDateTime.now().plusDays(1))
            .build();
        
        appointment.confirm();
        
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }
    
    @Test
    void confirm_whenAlreadyConfirmed_shouldThrowException() {
        Appointment appointment = Appointment.builder()
            .status(AppointmentStatus.CONFIRMED)
            .build();
        
        assertThatThrownBy(appointment::confirm)
            .isInstanceOf(InvalidAppointmentStateException.class)
            .hasMessageContaining("Sadece bekleyen randevular onaylanabilir");
    }
    
    @Test
    void markAsRisky_shouldSetRiskyFlagAndReason() {
        Appointment appointment = createPendingAppointment();
        
        appointment.markAsRisky("Onay alınamadı");
        
        assertThat(appointment.isRisky()).isTrue();
        assertThat(appointment.getRiskReason()).isEqualTo("Onay alınamadı");
    }
}
```

### Value Object Tests
```java
class TimeSlotTest {

    @Test
    void constructor_whenStartAfterEnd_shouldThrowException() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 10, 15, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 10, 14, 0);
        
        assertThatThrownBy(() -> new TimeSlot(start, end))
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void overlapsWith_whenSlotsOverlap_shouldReturnTrue() {
        TimeSlot slot1 = new TimeSlot(
            LocalDateTime.of(2025, 1, 10, 10, 0),
            LocalDateTime.of(2025, 1, 10, 11, 0)
        );
        TimeSlot slot2 = new TimeSlot(
            LocalDateTime.of(2025, 1, 10, 10, 30),
            LocalDateTime.of(2025, 1, 10, 11, 30)
        );
        
        assertThat(slot1.overlapsWith(slot2)).isTrue();
    }
    
    @Test
    void overlapsWith_whenSlotsDoNotOverlap_shouldReturnFalse() {
        TimeSlot slot1 = new TimeSlot(
            LocalDateTime.of(2025, 1, 10, 10, 0),
            LocalDateTime.of(2025, 1, 10, 11, 0)
        );
        TimeSlot slot2 = new TimeSlot(
            LocalDateTime.of(2025, 1, 10, 11, 0),
            LocalDateTime.of(2025, 1, 10, 12, 0)
        );
        
        assertThat(slot1.overlapsWith(slot2)).isFalse();
    }
}
```

### Domain Service Tests
```java
class AvailabilityCalculatorTest {

    private AvailabilityCalculator calculator = new AvailabilityCalculator();
    
    @Test
    void calculateAvailableSlots_whenNoAppointments_shouldReturnAllWorkingHourSlots() {
        WorkingHours workingHours = WorkingHours.of(
            LocalTime.of(9, 0), LocalTime.of(18, 0)
        );
        ServiceDuration duration = new ServiceDuration(60);
        LocalDate date = LocalDate.of(2025, 1, 10);
        
        List<TimeSlot> slots = calculator.calculateAvailableSlots(
            workingHours,
            Collections.emptyList(),
            duration,
            date
        );
        
        assertThat(slots).hasSize(9); // 9 adet 1 saatlik slot
    }
    
    @Test
    void calculateAvailableSlots_withExistingAppointments_shouldExcludeOccupiedSlots() {
        WorkingHours workingHours = WorkingHours.of(
            LocalTime.of(9, 0), LocalTime.of(12, 0)
        );
        List<Appointment> existing = List.of(
            createAppointment(LocalTime.of(10, 0), LocalTime.of(11, 0))
        );
        ServiceDuration duration = new ServiceDuration(60);
        
        List<TimeSlot> slots = calculator.calculateAvailableSlots(
            workingHours,
            existing,
            duration,
            LocalDate.of(2025, 1, 10)
        );
        
        assertThat(slots).hasSize(2); // 09:00-10:00 ve 11:00-12:00
    }
}
```

---

## Service Layer Tests

### Using Mocks
```java
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository repository;
    
    @Mock
    private AvailabilityService availabilityService;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private AppointmentService service;
    
    @Test
    void confirmAppointment_shouldChangeStatusAndSendNotification() {
        Long appointmentId = 1L;
        Appointment appointment = createPendingAppointment(appointmentId);
        
        when(repository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        Appointment result = service.confirmAppointment(appointmentId);
        
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(notificationService).sendConfirmationNotification(appointment);
        verify(repository).save(appointment);
    }
    
    @Test
    void confirmAppointment_whenNotFound_shouldThrowException() {
        Long appointmentId = 999L;
        when(repository.findById(appointmentId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.confirmAppointment(appointmentId))
            .isInstanceOf(AppointmentNotFoundException.class);
    }
}
```

---

## Integration Tests

### Repository Tests
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository repository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void findByBusinessIdAndDateBetween_shouldReturnMatchingAppointments() {
        Business business = createAndPersistBusiness();
        Appointment app1 = createAndPersistAppointment(business, LocalDate.of(2025, 1, 10));
        Appointment app2 = createAndPersistAppointment(business, LocalDate.of(2025, 1, 15));
        createAndPersistAppointment(business, LocalDate.of(2025, 1, 25));
        
        List<Appointment> result = repository.findByBusinessIdAndScheduledAtBetween(
            business.getId(),
            LocalDate.of(2025, 1, 1).atStartOfDay(),
            LocalDate.of(2025, 1, 20).atTime(23, 59)
        );
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Appointment::getId)
            .containsExactlyInAnyOrder(app1.getId(), app2.getId());
    }
    
    @Test
    void findRiskyAppointments_shouldReturnOnlyRiskyOnes() {
        Business business = createAndPersistBusiness();
        Appointment risky = createAndPersistAppointment(business);
        risky.markAsRisky("Onay alınamadı");
        entityManager.persistAndFlush(risky);
        
        createAndPersistAppointment(business); // not risky
        
        List<Appointment> result = repository.findByBusinessIdAndRiskyTrue(business.getId());
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(risky.getId());
    }
}
```

### Service Integration Tests
```java
@SpringBootTest
@Transactional
class AppointmentServiceIntegrationTest {

    @Autowired
    private AppointmentService service;
    
    @Autowired
    private TestDataFactory testData;
    
    @Test
    void createAndConfirmAppointment_fullFlow() {
        Business business = testData.createBusiness();
        Customer customer = testData.createCustomer(business);
        Employee employee = testData.createEmployee(business);
        ServiceDefinition serviceDefinition = testData.createService(business, 60);
        
        CreateAppointmentRequest request = new CreateAppointmentRequest(
            business.getId(),
            customer.getId(),
            employee.getId(),
            serviceDefinition.getId(),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            null
        );
        
        Appointment created = service.createAppointment(request);
        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        
        Appointment confirmed = service.confirmAppointment(created.getId());
        assertThat(confirmed.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }
}
```

---

## Controller Tests

```java
@WebMvcTest(AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AppointmentService service;
    
    @MockBean
    private AppointmentMapper mapper;
    
    @Test
    void createAppointment_withValidRequest_shouldReturn201() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
            1L, 2L, 3L, 4L, LocalDateTime.now().plusDays(1), null, null, null
        );
        Appointment appointment = createAppointment();
        AppointmentResponse response = createAppointmentResponse();
        
        when(service.createAppointment(any())).thenReturn(appointment);
        when(mapper.toResponse(appointment)).thenReturn(response);
        
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1));
    }
    
    @Test
    void createAppointment_withInvalidRequest_shouldReturn400() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
            null, null, null, null, null, null, null, null
        );
        
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
    
    @Test
    void confirmAppointment_whenSlotConflict_shouldReturn409() throws Exception {
        when(service.confirmAppointment(1L))
            .thenThrow(new SlotNotAvailableException("Saat doldu"));
        
        mockMvc.perform(post("/api/v1/appointments/1/confirm"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("SLOT_NOT_AVAILABLE"));
    }
}
```

---

## Test Utilities

### Test Data Factory
```java
@Component
public class TestDataFactory {

    @Autowired
    private BusinessRepository businessRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    public Business createBusiness() {
        Business business = Business.builder()
            .name("Test İşletme")
            .slug("test-isletme")
            .phone(new PhoneNumber("+90", "5551234567"))
            .build();
        return businessRepository.save(business);
    }
    
    public Customer createCustomer(Business business) {
        Customer customer = Customer.builder()
            .businessId(business.getId())
            .name("Test Müşteri")
            .phone(new PhoneNumber("+90", "5559876543"))
            .build();
        return customerRepository.save(customer);
    }
    
    public Appointment createPendingAppointment(
        Business business, 
        Customer customer, 
        Employee employee
    ) {
        return Appointment.builder()
            .businessId(business.getId())
            .customerId(customer.getId())
            .employeeId(employee.getId())
            .status(AppointmentStatus.PENDING)
            .scheduledAt(LocalDateTime.now().plusDays(1))
            .build();
    }
}
```

### Custom Assertions
```java
public class AppointmentAssert extends AbstractAssert<AppointmentAssert, Appointment> {

    public static AppointmentAssert assertThat(Appointment appointment) {
        return new AppointmentAssert(appointment);
    }
    
    public AppointmentAssert isConfirmed() {
        isNotNull();
        if (actual.getStatus() != AppointmentStatus.CONFIRMED) {
            failWithMessage("Expected status to be CONFIRMED but was <%s>", 
                actual.getStatus());
        }
        return this;
    }
    
    public AppointmentAssert isRisky() {
        isNotNull();
        if (!actual.isRisky()) {
            failWithMessage("Expected appointment to be risky");
        }
        return this;
    }
}
```

---

## Test Coverage Requirements

| Layer | Minimum Coverage |
|-------|-----------------|
| Domain (Entity, Value Object) | 90% |
| Service | 80% |
| Controller | 70% |
| Repository | 60% (mainly custom queries) |

### Mutation Testing (Future)
- PIT mutation testing for domain layer
- Minimum mutation score: 70%

---

## Testing Best Practices

### DO ✅
- Test one thing per test method
- Use descriptive test names
- Keep tests independent
- Use builders for test data
- Mock external dependencies

### DON'T ❌
- Share mutable state between tests
- Test implementation details
- Write flaky tests
- Skip edge cases
- Ignore test failures
