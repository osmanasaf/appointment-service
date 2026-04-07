WhatsApp-First Randevu ve Takip Sistemi

1. Proje Amacı

Sağlık ve güzellik profesyonellerinin (psikolog, diyetisyen, kuaför, güzellik merkezi, protez tırnak vb.) gelmeyen randevularını azaltmak, paket/seans takibini otomatikleştirmek ve WhatsApp üzerinden randevu onayı & hatırlatma ile operasyonel yüklerini minimuma indirmek.

Temel değer önerisi:

“Gelmeyen randevuları azaltır, boş saatleri kurtarır.”

2. Hedef Kitle

Psikolog (tek çalışan, seans bazlı)

Diyetisyen (paket + kontrol randevuları)

Kuaför (çoklu hizmet, yoğun takvim)

Güzellik merkezi / protez tırnak (paket, tekrar randevu)

Ortak problemler:

WhatsApp üzerinden manuel randevu

No-show (gelmeyen müşteri)

Paket/seans karmaşası

Boş saatlerin geç fark edilmesi

3. Ürün Kapsamı (Scope)

3.1 Çekirdek Yaklaşım

Marketplace yok (müşteri getirme vaadi yok)

B2B SaaS (işletmenin kendi müşterisi)

WhatsApp-first iletişim

Mobil uyumlu web (ilk etapta mobil app yok)

4. Paketler ve Fiyatlandırma (Özet)

PRO START – 499 TL / ay (Ana Paket)

Online randevu linki

Takvim & boş saat yönetimi

Paket / seans takibi

WhatsApp randevu onayı (1 gün önce)

Riskli randevu uyarısı (onaylamayanlar)

1–2 çalışan (küçük ekipler için ideal)

Opsiyonel Add-on’lar

WhatsApp mesajları: 4 TL / mesaj (kullandıkça öde)

Gelişmiş No-show Koruma: +199 TL / ay

5. Sistem Mimarisi (Yüksek Seviye)

Frontend: Web (React / Vue – mobil uyumlu)

Backend: REST API (Spring Boot veya FastAPI)

DB: PostgreSQL

Bildirim: WhatsApp Business API (+ fallback SMS)

Auth: İşletme bazlı login

6. Ana Modüller ve Feature Listesi

Temel prensip: Sistem slot bazlı değil, işlem (hizmet) süresi bazlı çalışır. Her işlem kendi süresine göre takvimde zaman bloklar. 30 dk – 6 saat arası tüm senaryolar native olarak desteklenir.

6.1 İşletme Yönetimi

İşletme profili (logo, ad, iletişim)

Çalışan tanımı (ad, çalışma saatleri)

Çalışma saatleri & kapalı günler

Onay kuralları (otomatik / manuel)

İşletme profili (logo, ad, iletişim)

Çalışan tanımı (ad, çalışma saatleri)

Hizmet tanımı (süre, opsiyonel fiyat)

6.2 İşlem / Hizmet Tanımlama (ÇEKİRDEK MODÜL)

İşletme, sunduğu tüm işlemleri ve sürelerini tanımlar.

Her işlem için alanlar:

İşlem adı (örn. Terapi seansı, Saç kesimi, Protez tırnak)

Süre (dakika bazlı, 15–360 dk)

Buffer süresi (opsiyonel, öncesi/sonrası)

Aynı gün kuralı (anında / onaylı / kapalı)

Manuel onay gereksinimi

Paket / seans uyumu

Aktif / pasif

Slot yoktur. Takvim, işlem süresine göre dinamik zaman aralığı hesaplar.

6.3 Online Randevu Modülü (Müşteri Tarafı)

Amaç: Müşterinin WhatsApp yazmadan randevu alabilmesi.

Akış:

İşletmeye özel randevu linki

İşlem seçimi (süre sistem tarafından bilinir)

Takvimde akıllı saat önerisi (yalnızca gerçekten sığan saatler)

Saatlerin yanında etiket:

🟢 Anında onay

⚠️ İşletme onayı

Ad + telefon girişi

Randevu oluşturma / talep gönderme

WhatsApp bilgilendirme

6.4 Takvim & Randevu Yönetimi

Günlük / haftalık takvim görünümü

Randevular başlangıç–bitiş zamanı ile gösterilir

Uzun işlemler (2–6 saat) tek blok olarak görünür

Onaylanan / bekleyen / riskli randevular

Manuel randevu ekleme

İptal / yeniden açma
(İşletme Tarafı)

Günlük / haftalık takvim görünümü

Onaylanan / bekleyen / riskli randevular

Manuel randevu ekleme

İptal / erteleme

6.5 Paket / Seans Takibi

Paket tanımı (örn. 5 / 10 / 20 seans)

İşlem türü ile ilişkilendirme

Randevu sonrası otomatik seans düşümü

Kalan seans görüntüleme

Paket bitiş uyarısı

Yeni paket öneri bildirimi

Paket tanımı (örn. 10 seans)

Randevu sonrası otomatik düşüm

Kalan seans görüntüleme

Paket bitiş uyarısı

Yeni paket öneri bildirimi

6.6 WhatsApp Onay & Hatırlatma Sistemi (Çekirdek Değer)

Zamanlama:

Varsayılan: Randevudan 24 saat önce

Aynı gün randevular için: Anında bilgilendirme + gerekirse onay

Akış:

Onay isteği (Geliyorum / İptal)

Onaylanmayan randevu → riskli işaretlenir

Zamanlama:

Randevudan 24 saat önce otomatik WhatsApp mesajı

Mesaj İçeriği (örnek):

“Yarın saat 15:00’te randevunuz var. Onaylıyor musunuz?”

Aksiyonlar:

✅ Geliyorum → randevu onaylanır

❌ İptal → randevu iptal, saat boşa düşer

6.7 Riskli Randevu & Akıllı Saat Yönetimi

Onaylamayan randevular riskli olarak işaretlenir

Aynı gün + uzun işlem randevuları otomatik riskli

İşletmeye WhatsApp / panel bildirimi

Akıllı Saat Algoritması:

İşlem süresi + buffer

Çalışma saatleri

Mevcut randevular

Onay kuralları

Çıktı: Sadece gerçekten mümkün saatler

Onaylamayan randevular riskli olarak işaretlenir

İşletme panelinde uyarı

Amaç: No-show’u önceden fark etmek

6.7 Gelişmiş No-show Koruma (Add-on)

Daha önce gelmeyen müşteri flag’i

Risk skoru (basit)

Son dakika boşluk önerisi

Waitlist (manuel / yarı otomatik)

7. Kullanıcı Akışları (Flow)

Sistem iki tip randevu destekler:

Otomatik onaylı

İşletme onaylı (talep)

7.1 İşletme Onboarding

Kayıt

İşletme bilgileri

Çalışma saatleri

İşlem / hizmet tanımları (süreler dahil)

Onay kuralları

WhatsApp entegrasyonu

Randevu linki aktif

Kayıt

İşletme bilgileri

Çalışma saatleri

Hizmet tanımı

WhatsApp entegrasyonu

Randevu linki aktif

7.2 Müşteri Randevu Akışı

Linke giriş

İşlem seçimi

Akıllı saat önerileri

Saat seçimi

Bilgi girişi

Randevu oluşur veya talep gönderilir

WhatsApp bilgilendirme

Linke giriş

Hizmet seçimi

Saat seçimi

Bilgi girişi

Randevu oluşturma

WhatsApp bilgilendirme

7.3 Randevu Öncesi Akış

T-24 saat WhatsApp onay mesajı

Geliyorum / İptal

Saat boşa düşerse otomatik açılır

T-24 saat: WhatsApp onay mesajı

Müşteri aksiyonu (onay / iptal)

İşletmeye durum bildirimi

8. MVP Kapsamı (İlk Versiyon – NETLEŞTİRİLDİ)

MVP’de OLACAKLAR (zorunlu)

Online randevu linki

Takvim (günlük / haftalık)

İşlem bazlı süre tanımı (15 dk – 6 saat)

Süreye göre akıllı saat önerisi

Otomatik / manuel onaylı randevu

Aynı gün randevu talebi

Paket / seans takibi

WhatsApp randevu onayı (T-24)

WhatsApp üzerinden iptal

Riskli randevu uyarısı (onaylanmayanlar)

MVP’de OLMAYACAKLAR

Mobil app

Marketplace (müşteri getirme)

Muhasebe / fatura

Kaynak paylaşımı (oda, koltuk vb.)

Gelişmiş AI raporlar

9. Başarı Metrikleri (KPI)

No-show oranı (önce / sonra)

Aylık randevu sayısı

WhatsApp onay oranı

Churn (iptal eden işletme)

10. Yol Haritası (Özet)

Ay 1: MVP geliştirme
Ay 2: 10–20 pilot müşteri
Ay 3: Paket & fiyat optimizasyonu
Ay 4+: Gelişmiş no-show ve raporlar

11. Özet

Bu sistem:

Slot bazlı değil, işlem süresi bazlı çalışır

30 dk’dan 6 saate kadar tüm işlemleri destekler

Akıllı saat önerisi ile gerçek müsaitlik gösterir

Aynı gün ve uzun işlemleri kontrollü yönetir

WhatsApp-first onay ile no-show’u düşürür

Sonuç: Gerçek hayatta çalışan, esnek ve ölçeklenebilir bir randevu SaaS.

Bu proje:

Kanıtlanmış bir ihtiyacı çözer

WhatsApp-first yaklaşımıyla rakiplerinden ayrışır

499 TL giriş + yan kalemlerle sürdürülebilir gelir üretir

Amaç: Basit, sessiz, para kurtaran bir sistem.

