ALTER TABLE business_subscriptions
    ADD COLUMN IF NOT EXISTS last_reminder_days_before INTEGER;
