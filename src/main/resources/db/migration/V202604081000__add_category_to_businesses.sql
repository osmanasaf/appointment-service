ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'OTHER';

CREATE INDEX IF NOT EXISTS idx_businesses_category ON businesses (category);
