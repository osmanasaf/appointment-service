CREATE TABLE IF NOT EXISTS reviews (
    id             BIGSERIAL PRIMARY KEY,
    business_id    BIGINT      NOT NULL,
    appointment_id BIGINT      NOT NULL,
    customer_id    BIGINT      NOT NULL,
    employee_id    BIGINT,
    rating         INT         NOT NULL,
    comment        VARCHAR(1000),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP,
    CONSTRAINT uq_reviews_appointment_id UNIQUE (appointment_id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_status CHECK (status IN ('PENDING_APPROVAL', 'PUBLISHED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_reviews_business_status
    ON reviews (business_id, status);

ALTER TABLE public_action_tokens DROP CONSTRAINT IF EXISTS chk_public_action_tokens_type;
ALTER TABLE public_action_tokens
    ADD CONSTRAINT chk_public_action_tokens_type CHECK (type IN ('RESCHEDULE', 'WAITLIST_OFFER', 'REVIEW'));

ALTER TABLE notification_settings
    ADD COLUMN IF NOT EXISTS review_request_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS review_request_delay_hours  INT     NOT NULL DEFAULT 2;

ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS google_review_url VARCHAR(500);
