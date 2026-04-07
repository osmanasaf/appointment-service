# Modül 03: PostgreSQL Production Konfigürasyonu

## Amaç
Production ortamında PostgreSQL kullanımı; H2 sadece geliştirme için kalsın. Profile ile ayrım (dev: H2, prod: PostgreSQL).

## Kabul kriterleri
- [ ] `application-prod.properties` (veya application-prod.yml) oluşturulur.
- [ ] Prod profile'da: spring.datasource.url (PostgreSQL JDBC), username, password, driver-class-name=org.postgresql.Driver tanımlıdır.
- [ ] JPA: Hibernate dialect `org.hibernate.dialect.PostgreSQLDialect`; ddl-auto prod'da `validate` veya `none` önerilir.
- [ ] pom.xml'de PostgreSQL driver dependency vardır (scope runtime).
- [ ] Dokümanda: Şifre için env variable kullanımı (örn. SPRING_DATASOURCE_PASSWORD) ve `spring.profiles.active=prod` örnek kullanımı yer alır.

## Etkilenen / yeni dosyalar
- `src/main/resources/application-prod.properties` (yeni)
- `pom.xml` — PostgreSQL driver dependency
- `md-files/modules/03-postgresql-production.md` — bu dosyada "Kullanım" bölümü

## Teknik notlar
- Mevcut application.properties H2 kullanıyor; default profile değiştirilmez; prod için ayrı profile kullanılır.
- URL format: `jdbc:postgresql://host:5432/dbname`
- Production'da ddl-auto=update riskli olabilir; validate veya none + migration (Flyway/Liquibase) tercih edilir. Bu modülde sadece validate yeterli.

## Alt görevler
- [ ] pom.xml'e postgresql dependency ekle
- [ ] application-prod.properties oluştur (url, username, password placeholder, dialect, ddl-auto=validate)
- [ ] Bu .md dosyasına "Kullanım" bölümü ekle (env, profile aktifleştirme)

## Kullanım
- Profile aktifleştirme: `spring.profiles.active=prod` (env: `SPRING_PROFILES_ACTIVE=prod` veya JVM `-Dspring.profiles.active=prod`).
- Şifre env ile: `SPRING_DATASOURCE_PASSWORD=yourpassword`; application-prod.properties'te `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}` kullanılabilir.
- Örnek URL: `jdbc:postgresql://localhost:5432/appointmentdb`
