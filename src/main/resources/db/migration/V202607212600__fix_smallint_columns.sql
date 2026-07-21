-- Invoice.periodYear/periodMonth ve StoredCard.expiryMonth/expiryYear entity'lerinde
-- plain int (Hibernate varsayılanı: INTEGER) olarak tanımlı, ama V202606131000 bunları
-- yanlışlıkla SMALLINT olarak oluşturmuştu. Tablolar henüz veri içermediğinden (bu deploy'da
-- ilk kez oluşturuldu) tip değişimi güvenli.
ALTER TABLE invoices
    ALTER COLUMN period_year TYPE INTEGER,
    ALTER COLUMN period_month TYPE INTEGER;

ALTER TABLE stored_cards
    ALTER COLUMN expiry_month TYPE INTEGER,
    ALTER COLUMN expiry_year TYPE INTEGER;
