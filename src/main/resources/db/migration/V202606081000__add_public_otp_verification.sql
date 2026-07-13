CREATE TABLE public_otp_verification (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    phone_normalized VARCHAR(20) NOT NULL,
    otp_code VARCHAR(6),
    otp_expiry TIMESTAMP,
    otp_attempts INT NOT NULL DEFAULT 0,
    otp_blocked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_public_otp_business_phone UNIQUE (business_id, phone_normalized)
);

CREATE INDEX idx_public_otp_business_phone ON public_otp_verification (business_id, phone_normalized);
