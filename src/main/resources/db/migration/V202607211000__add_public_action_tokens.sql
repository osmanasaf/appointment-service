CREATE TABLE IF NOT EXISTS public_action_tokens (
    id             BIGSERIAL PRIMARY KEY,
    business_id    BIGINT       NOT NULL,
    appointment_id BIGINT       NOT NULL,
    type           VARCHAR(32)  NOT NULL,
    token          VARCHAR(64)  NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    expires_at     TIMESTAMP    NOT NULL,
    used_at        TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP,
    CONSTRAINT uq_public_action_tokens_token UNIQUE (token),
    CONSTRAINT chk_public_action_tokens_type CHECK (type IN ('RESCHEDULE')),
    CONSTRAINT chk_public_action_tokens_status CHECK (status IN ('PENDING', 'USED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_public_action_tokens_appointment
    ON public_action_tokens (appointment_id);
CREATE INDEX IF NOT EXISTS idx_public_action_tokens_business
    ON public_action_tokens (business_id);
