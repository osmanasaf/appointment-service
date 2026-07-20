ALTER TABLE waitlist_entries
    ADD COLUMN IF NOT EXISTS converted_at TIMESTAMP;

ALTER TABLE notification_settings
    ADD COLUMN IF NOT EXISTS monthly_summary_enabled BOOLEAN NOT NULL DEFAULT TRUE;
