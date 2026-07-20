-- public_action_tokens: appointment_id -> subject_id (RESCHEDULE ve WAITLIST_OFFER
-- tipleri farklı varlıkları (appointment / waitlist entry) işaret edebilsin diye)
ALTER TABLE public_action_tokens RENAME COLUMN appointment_id TO subject_id;

ALTER TABLE public_action_tokens DROP CONSTRAINT IF EXISTS chk_public_action_tokens_type;
ALTER TABLE public_action_tokens
    ADD CONSTRAINT chk_public_action_tokens_type CHECK (type IN ('RESCHEDULE', 'WAITLIST_OFFER'));

CREATE TABLE IF NOT EXISTS waitlist_entries (
    id                    BIGSERIAL PRIMARY KEY,
    business_id           BIGINT      NOT NULL,
    customer_id           BIGINT      NOT NULL,
    service_id            BIGINT      NOT NULL,
    employee_id           BIGINT,
    preferred_from        DATE,
    preferred_until       DATE,
    status                VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    notified_at           TIMESTAMP,
    offered_employee_id   BIGINT,
    offered_scheduled_at  TIMESTAMP,
    offered_end_at        TIMESTAMP,
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    CONSTRAINT chk_waitlist_entries_status
        CHECK (status IN ('WAITING', 'NOTIFIED', 'CONVERTED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_waitlist_entries_business_status
    ON waitlist_entries (business_id, status);
CREATE INDEX IF NOT EXISTS idx_waitlist_entries_service
    ON waitlist_entries (business_id, service_id, status);
