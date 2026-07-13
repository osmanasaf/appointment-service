-- ============================================================
-- Migration: PayTR ödeme entegrasyonu için yeni tablolar
-- + business_subscriptions tablosuna ödeme alanları eklendi
-- ============================================================

-- 1. business_subscriptions — yeni ödeme alanları
ALTER TABLE business_subscriptions
    ADD COLUMN IF NOT EXISTS billing_period        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS auto_renew            BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS next_payment_date     DATE,
    ADD COLUMN IF NOT EXISTS last_payment_date     DATE,
    ADD COLUMN IF NOT EXISTS paytr_utoken_encrypted VARCHAR(512);

-- 2. payment_transactions — her ödeme girişiminin kaydı
CREATE TABLE IF NOT EXISTS payment_transactions (
    id               BIGSERIAL PRIMARY KEY,
    business_id      BIGINT        NOT NULL,
    merchant_oid     VARCHAR(64)   NOT NULL,
    payment_amount   NUMERIC(10,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL DEFAULT 'TL',
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_type     VARCHAR(30)   NOT NULL,
    plan_code        VARCHAR(30)   NOT NULL,
    billing_period   VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY',
    paytr_token      VARCHAR(512),
    error_message    VARCHAR(512),
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP,
    CONSTRAINT uq_payment_transactions_merchant_oid UNIQUE (merchant_oid),
    CONSTRAINT chk_payment_status
        CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    CONSTRAINT chk_payment_type
        CHECK (payment_type IN ('NEW_SUBSCRIPTION','RENEWAL','UPGRADE','DOWNGRADE')),
    CONSTRAINT chk_payment_billing_period
        CHECK (billing_period IN ('MONTHLY','YEARLY'))
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_business_id
    ON payment_transactions (business_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status
    ON payment_transactions (business_id, status);

-- 3. stored_cards — PayTR'den alınan şifrelenmiş ctoken'lar
CREATE TABLE IF NOT EXISTS stored_cards (
    id                  BIGSERIAL PRIMARY KEY,
    business_id         BIGINT       NOT NULL,
    ctoken_encrypted    VARCHAR(512) NOT NULL,
    last4               VARCHAR(4)   NOT NULL,
    card_brand          VARCHAR(30),
    expiry_month        SMALLINT     NOT NULL,
    expiry_year         SMALLINT     NOT NULL,
    is_default          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stored_cards_business_id
    ON stored_cards (business_id);

-- 4. invoices — ödeme sonrası oluşturulan fatura kayıtları
CREATE TABLE IF NOT EXISTS invoices (
    id              BIGSERIAL PRIMARY KEY,
    business_id     BIGINT        NOT NULL,
    transaction_id  BIGINT,
    invoice_number  VARCHAR(50)   NOT NULL,
    amount          NUMERIC(10,2) NOT NULL,
    tax_amount      NUMERIC(10,2) NOT NULL DEFAULT 0,
    period_year     SMALLINT      NOT NULL,
    period_month    SMALLINT      NOT NULL,
    pdf_url         VARCHAR(1024),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    CONSTRAINT uq_invoices_invoice_number UNIQUE (invoice_number)
);

CREATE INDEX IF NOT EXISTS idx_invoices_business_id
    ON invoices (business_id);
CREATE INDEX IF NOT EXISTS idx_invoices_transaction_id
    ON invoices (transaction_id);

-- 5. payment_retries — başarısız tekrarlayan ödemelerin yeniden deneme takibi
CREATE TABLE IF NOT EXISTS payment_retries (
    id              BIGSERIAL PRIMARY KEY,
    business_id     BIGINT      NOT NULL,
    attempt_count   INT         NOT NULL DEFAULT 0,
    next_retry_date DATE,
    last_error      VARCHAR(512),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    CONSTRAINT uq_payment_retries_business_id UNIQUE (business_id)
);

CREATE INDEX IF NOT EXISTS idx_payment_retries_next_retry_date
    ON payment_retries (next_retry_date)
    WHERE next_retry_date IS NOT NULL;
