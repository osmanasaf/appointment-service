ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS google_event_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS google_calendar_synced_at TIMESTAMP;

ALTER TABLE public_action_tokens DROP CONSTRAINT IF EXISTS chk_public_action_tokens_type;
ALTER TABLE public_action_tokens
    ADD CONSTRAINT chk_public_action_tokens_type
    CHECK (type IN ('RESCHEDULE', 'WAITLIST_OFFER', 'REVIEW', 'CALENDAR_ICS', 'GOOGLE_OAUTH_STATE'));

CREATE TABLE IF NOT EXISTS calendar_connections (
    id                        BIGSERIAL PRIMARY KEY,
    business_id               BIGINT       NOT NULL,
    employee_id               BIGINT       NOT NULL,
    google_calendar_id        VARCHAR(255) NOT NULL DEFAULT 'primary',
    refresh_token_encrypted   VARCHAR(1000) NOT NULL,
    access_token_encrypted    VARCHAR(1000),
    access_token_expires_at   TIMESTAMP,
    status                    VARCHAR(16)  NOT NULL DEFAULT 'CONNECTED',
    broken_reason             VARCHAR(500),
    last_synced_at            TIMESTAMP,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP,
    CONSTRAINT uq_calendar_connections_employee_id UNIQUE (employee_id),
    CONSTRAINT chk_calendar_connections_status CHECK (status IN ('CONNECTED', 'BROKEN', 'DISCONNECTED'))
);

CREATE TABLE IF NOT EXISTS employee_external_busy (
    id                BIGSERIAL PRIMARY KEY,
    business_id       BIGINT       NOT NULL,
    employee_id       BIGINT       NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    start_at          TIMESTAMP    NOT NULL,
    end_at            TIMESTAMP    NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_employee_external_busy_employee_range
    ON employee_external_busy (employee_id, start_at, end_at);
