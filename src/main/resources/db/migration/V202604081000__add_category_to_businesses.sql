ALTER TABLE businesses
    ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'OTHER';

CREATE INDEX idx_businesses_category ON businesses (category);
