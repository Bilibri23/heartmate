CREATE INDEX IF NOT EXISTS idx_payments_momo_transaction_status
    ON payments(LOWER(TRIM(momo_transaction_id)), status)
    WHERE momo_transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_proof_url_status
    ON payments(TRIM(payment_proof_url), status)
    WHERE payment_proof_url IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_analytics_event_ai_no_answer_question
    ON analytics_event((metadata ->> 'question'), role, created_at DESC)
    WHERE event_type = 'ai_no_answer';
