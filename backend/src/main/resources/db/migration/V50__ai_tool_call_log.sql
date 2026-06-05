CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS ai_tool_call_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    role VARCHAR(40),
    tool_name VARCHAR(100) NOT NULL,
    allowed BOOLEAN NOT NULL DEFAULT TRUE,
    result_summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_tool_call_log_created_at ON ai_tool_call_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_tool_call_log_user_created ON ai_tool_call_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_tool_call_log_tool_created ON ai_tool_call_log(tool_name, created_at DESC);
