-- Curated finetuning dataset for the RoomBay assistant.
-- Each row is a single supervised example: (system, user, sanitized_context, ideal_assistant)
-- Tagged with kind so we can sample style / refusal / fallback / structured rows separately.

CREATE TABLE IF NOT EXISTS ai_finetune_examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- task taxonomy
    kind         VARCHAR(40) NOT NULL,         -- STYLE | REFUSAL | FALLBACK | STRUCTURED | RECOMMENDATION
    persona      VARCHAR(20) NOT NULL,         -- TENANT | LANDLORD | NEUTRAL
    tags         TEXT[]      NOT NULL DEFAULT '{}',

    -- supervised pair (already sanitized; safe to ship to a vendor)
    system_prompt        TEXT NOT NULL,
    user_message         TEXT NOT NULL,
    sanitized_context    TEXT,                 -- optional retrieved context, redacted
    ideal_assistant      TEXT NOT NULL,

    -- training response_format hint: "text" or "json_object"
    response_format      VARCHAR(20) NOT NULL DEFAULT 'text',

    -- provenance / curation
    source        VARCHAR(60) NOT NULL DEFAULT 'manual', -- manual | seed | log_curated
    notes         TEXT,
    is_active     BOOLEAN     NOT NULL DEFAULT true,
    created_by    UUID,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_finetune_examples_kind ON ai_finetune_examples(kind);
CREATE INDEX IF NOT EXISTS idx_ai_finetune_examples_persona ON ai_finetune_examples(persona);
CREATE INDEX IF NOT EXISTS idx_ai_finetune_examples_active ON ai_finetune_examples(is_active);
