# Modül 04: Unit ve Entegrasyon Testleri

## Amaç
Kritik servis ve akışlar için unit ve entegrasyon testleri yazmak; proje testing.md kurallarına (isimlendirme: methodName_givenCondition_expectedResult, AAA pattern) uyum.

## Kabul kriterleri
- [ ] AvailabilityService: Müsait slot hesaplama en az bir senaryoda test edilir (örn. çalışma saati içinde, mevcut randevu yokken slotlar dönülür; çakışan randevu varken o slot dönülmez).
- [ ] AppointmentService: create, confirm, cancel, complete, markAsRisky en az birer test (örn. confirm_whenPendingAppointment_shouldChangeStatusToConfirmed).
- [ ] NotificationService: sendAppointmentReminder çağrıldığında WhatsAppClient ve log davranışı mock'lanmış en az bir unit test.
- [ ] (Opsiyonel) PublicAppointmentController veya AppointmentController için @WebMvcTest ile bir endpoint testi.
- [ ] Tüm testler mevcut AppointmentServiceApplicationTests ile birlikte yeşil geçer.

## Etkilenen / yeni dosyalar
- `src/test/java/com/asaf/appointment_service/service/appointment/AvailabilityServiceTest.java` (yeni)
- `src/test/java/com/asaf/appointment_service/service/appointment/AppointmentServiceTest.java` (yeni)
- `src/test/java/com/asaf/appointment_service/service/notification/NotificationServiceTest.java` (yeni)
- (Opsiyonel) `src/test/java/com/asaf/appointment_service/controller/appointment/PublicAppointmentControllerTest.java` veya AppointmentControllerTest

## Teknik notlar
- testing.md: Unit test naming `methodName_givenCondition_expectedResult`, AAA (Arrange, Act, Assert).
- AvailabilityService: BusinessWorkingHoursRepository, AppointmentRepository mock'lanır; çalışma saati ve mevcut randevular verilerek getAvailableSlots sonucu assert edilir.
- AppointmentService: Repository mock; entity builder ile PENDING randevu, confirm() sonrası status CONFIRMED.
- NotificationService: WhatsAppClient mock; sendAppointmentReminder çağrısında sendAndLog veya send çağrıldığı doğrulanır.
- JUnit 5, Mockito; Spring Boot Test slice (@DataJpaTest, @WebMvcTest) veya @MockBean ile @SpringBootTest kullanılabilir.

## Alt görevler
- [ ] AvailabilityServiceTest: getAvailableSlots için en az 1 senaryo
- [ ] AppointmentServiceTest: confirm, cancel, complete için en az birer test
- [ ] NotificationServiceTest: sendAppointmentReminder mock test
- [ ] (Opsiyonel) Controller test
