# Modül 02: Paket Seans Düşümü

## Amaç
Randevu COMPLETED olduğunda, ilgili hizmet paket uyumlu ise (packageEligible) ve müşteriye ait o hizmette aktif paket varsa otomatik olarak bir seans düşülmesi. Kalan seans yoksa veya paket süresi dolmuşsa uygun exception fırlatılır.

## Kabul kriterleri
- [ ] `AppointmentService.completeAppointment` çağrıldığında, randevunun serviceId'sine ait ServiceDefinition.packageEligible true ise paket kontrolü yapılır.
- [ ] Aynı müşteri (customerId) ve aynı hizmet (serviceId) için tek bir aktif (active=true, remainingSessions > 0, süresi dolmamış) CustomerPackage varsa `PackageService.deductSession(packageId)` çağrılır.
- [ ] Birden fazla aktif paket varsa: en erken biten (remainingSessions en düşük veya expiresAt en yakın) kullanılabilir; dokümanda "tek aktif paket" tercih edildiği için ilk bulunan aktif paket kullanılabilir.
- [ ] Aktif paket yoksa: randevu yine COMPLETED yapılır, seans düşümü yapılmaz (hata fırlatılmaz).
- [ ] Paket var ama kalan seans 0 veya süresi dolmuşsa: Randevu tamamlanmadan önce kontrol; uygunsa `InsufficientSessionsException` (veya mevcut IllegalStateException) fırlatılabilir veya sadece düşüm yapılmaz ve randevu tamamlanır. Modül kararı: Paket varsa ve kalan seans > 0 ise düş; yoksa randevu yine tamamlanır (no exception).
- [ ] exception/package/InsufficientSessionsException tanımlanır; GlobalExceptionHandler'da map edilir (opsiyonel, ileride paket zorunluluğu için).

## Etkilenen / yeni dosyalar
- `src/main/java/com/asaf/appointment_service/service/appointment/AppointmentService.java` — completeAppointment içinde paket kontrolü ve deductSession çağrısı
- `src/main/java/com/asaf/appointment_service/service/customer/PackageService.java` — (gerekirse) findByCustomerIdAndServiceIdAndActive gibi metod için repository kullanımı
- `src/main/java/com/asaf/appointment_service/repository/customer/CustomerPackageRepository.java` — findByCustomerIdAndServiceIdAndActiveTrue (ve remainingSessions > 0, not expired) gibi
- `src/main/java/com/asaf/appointment_service/exception/package/InsufficientSessionsException.java` (yeni, opsiyonel)
- `src/main/java/com/asaf/appointment_service/common/exception/GlobalExceptionHandler.java` — InsufficientSessionsException handle (opsiyonel)

## Teknik notlar
- PackageService.deductSession(packageId) zaten var; CustomerPackage.deductSession() kalan 0 olunca active=false yapıyor.
- Randevu oluşturulurken paket seçimi zorunlu değil; tamamlanırken "customerId + serviceId" ile aktif paket aranır.
- CustomerPackageRepository'de: findByCustomerIdAndServiceIdAndActiveTrue, expiresAt > now (veya expiresAt null) ve remainingSessions > 0 filtreleri gerekebilir. Mevcut repository'yi kontrol et.

## Alt görevler
- [ ] CustomerPackageRepository: aktif paket bulma metodu (customerId, serviceId)
- [ ] AppointmentService: completeAppointment içinde ServiceDefinition.packageEligible kontrolü
- [ ] AppointmentService: uygun paket varsa PackageService.deductSession çağrısı
- [ ] (Opsiyonel) InsufficientSessionsException ve handler
