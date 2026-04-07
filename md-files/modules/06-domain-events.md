# Modül 06: Domain Events (Opsiyonel)

## Amaç
Randevu oluşturulduğunda, onaylandığında veya iptal edildiğinde in-process domain event yayımlamak; ileride bildirim, raporlama veya entegrasyon dinleyicıları için temel sağlamak.

## Kabul kriterleri
- [x] AppointmentCreated, AppointmentConfirmed, AppointmentCancelled (ve gerekirse AppointmentCompleted) event sınıfları tanımlanır (domain/event veya common/event).
- [x] Spring ApplicationEventPublisher kullanılarak AppointmentService içinde create, confirm, cancel (ve complete) noktalarında ilgili event publish edilir.
- [ ] (Opsiyonel) Bir listener örneği: Loglama veya NotificationService tetiklemesi; bu modülde sadece event yayımlama yeterli.

## Etkilenen / yeni dosyalar
- `src/main/java/com/asaf/appointment_service/domain/event/AppointmentCreated.java` (yeni)
- `src/main/java/com/asaf/appointment_service/domain/event/AppointmentConfirmed.java` (yeni)
- `src/main/java/com/asaf/appointment_service/domain/event/AppointmentCancelled.java` (yeni)
- `src/main/java/com/asaf/appointment_service/service/appointment/AppointmentService.java` — publish çağrıları

## Teknik notlar
- Event'ler ApplicationEvent'dan extend edebilir veya POJO + ApplicationEventPublisher.publishEvent(object).
- package-structure.md'de domain/event altında AppointmentCreated, AppointmentConfirmed, AppointmentCancelled geçiyor.
- Bu modül opsiyonel; Faz 2 zorunlu sırada yer almaz.

## Alt görevler
- [ ] Event sınıfları (appointmentId, businessId, customerId, scheduledAt vb. gerekli alanlar)
- [ ] AppointmentService'e ApplicationEventPublisher inject; create/confirm/cancel/complete sonrası publish
