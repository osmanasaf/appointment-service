ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS instagram_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tiktok_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS show_on_public_page BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS business_photos (
    id          BIGSERIAL PRIMARY KEY,
    business_id BIGINT       NOT NULL,
    url         VARCHAR(2048) NOT NULL,
    caption     VARCHAR(200),
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_business_photos_business_id ON business_photos (business_id);
