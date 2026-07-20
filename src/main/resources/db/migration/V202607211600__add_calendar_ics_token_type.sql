ALTER TABLE public_action_tokens DROP CONSTRAINT IF EXISTS chk_public_action_tokens_type;
ALTER TABLE public_action_tokens
    ADD CONSTRAINT chk_public_action_tokens_type
    CHECK (type IN ('RESCHEDULE', 'WAITLIST_OFFER', 'REVIEW', 'CALENDAR_ICS'));
