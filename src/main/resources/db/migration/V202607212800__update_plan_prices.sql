-- Paket fiyatlarini dusur: SOLO 490, PRO 690, ENTERPRISE 1290 (TL/ay)

UPDATE plans SET monthly_price = 490.00, yearly_price = 4900.00, updated_at = NOW() WHERE code = 'SOLO';
UPDATE plans SET monthly_price = 690.00, yearly_price = 6900.00, updated_at = NOW() WHERE code = 'PRO';
UPDATE plans SET monthly_price = 1290.00, yearly_price = 12900.00, updated_at = NOW() WHERE code = 'ENTERPRISE';
