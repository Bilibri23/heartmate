-- Add missing edit columns to messages table to match Message entity
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP;
