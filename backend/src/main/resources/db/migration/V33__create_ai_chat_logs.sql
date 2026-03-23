-- AI chat audit logs
CREATE TABLE IF NOT EXISTS ai_chat_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    persona TEXT NOT NULL,
    user_message TEXT NOT NULL,
    assistant_answer TEXT NOT NULL,
    citation_chunk_ids jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

